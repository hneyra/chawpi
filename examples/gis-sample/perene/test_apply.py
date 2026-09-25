"""End-to-end tests for apply.py's CLI against a fake Core.

Run: cd examples/gis-sample/perene && python3 -m unittest -v
"""
import io
import json
import os
import socket
import threading
import unittest
from contextlib import redirect_stdout, redirect_stderr
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

import apply

MODEL_PATH = os.path.join(os.path.dirname(__file__), "model.json")


class FakeCoreHandler(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass  # keep test output quiet

    def _body(self):
        length = int(self.headers.get("Content-Length", 0))
        raw = self.rfile.read(length) if length else b""
        return json.loads(raw) if raw else None

    def _handle(self, method):
        server = self.server
        server.requests.append((method, self.path, self.headers.get("Authorization"), self._body()))
        status, payload = server.script(method, self.path)
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        if payload is not None:
            self.wfile.write(json.dumps(payload).encode("utf-8"))

    def do_GET(self):
        self._handle("GET")

    def do_POST(self):
        self._handle("POST")

    def do_PUT(self):
        self._handle("PUT")

    def do_DELETE(self):
        self._handle("DELETE")


class FakeCore:
    """Scripted fake Core: existing_objects/existing_relationships mark names that 409 on create."""

    def __init__(self, existing_objects=(), existing_relationships=(), fail_on_post_object=None,
                 fail_put=False, fail_put_status=500, login_response=None):
        self.existing_objects = set(existing_objects)
        self.existing_relationships = set(existing_relationships)
        self.fail_on_post_object = fail_on_post_object  # object name -> triggers 500
        self.fail_put = fail_put  # PUT .../fields/... -> fail_put_status
        self.fail_put_status = fail_put_status
        self.login_response = login_response  # override the default {"token": "t"}
        self.requests = []
        self.server = ThreadingHTTPServer(("127.0.0.1", 0), FakeCoreHandler)
        self.server.requests = self.requests
        self.server.script = self._script
        self.port = self.server.server_address[1]
        self.base_url = f"http://127.0.0.1:{self.port}"
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()

    def stop(self):
        self.server.shutdown()
        self.server.server_close()

    def _script(self, method, path):
        if path == "/api/auth/login" and method == "POST":
            if self.login_response is not None:
                return 200, self.login_response
            return 200, {"token": "t"}
        if path == "/api/objects" and method == "GET":
            return 200, [{"name": n} for n in self.existing_objects]
        if path == "/api/objects" and method == "POST":
            body = self.requests[-1][3]
            name = body["name"]
            if self.fail_on_post_object and name == self.fail_on_post_object:
                return 500, {"message": "boom"}
            if name in self.existing_objects:
                return 409, {"message": "exists"}
            return 201, {"name": name}
        if path == "/api/relationships" and method == "POST":
            body = self.requests[-1][3]
            name = body["name"]
            if name in self.existing_relationships:
                return 409, {"message": "exists"}
            return 201, {"name": name}
        if path.startswith("/api/metadata/objects/") and method == "PUT":
            if self.fail_put:
                return self.fail_put_status, {"message": "boom-put"}
            return 200, {"required": True}
        if path.startswith("/api/relationships/") and method == "DELETE":
            name = path.rsplit("/", 1)[-1]
            if name in self.deleted_missing_relationships():
                return 404, {"message": "not found"}
            return 204, None
        if path.startswith("/api/objects/") and method == "DELETE":
            name = path.rsplit("/", 1)[-1]
            if name in self.deleted_missing_objects():
                return 404, {"message": "not found"}
            return 204, None
        return 500, {"message": "boom"}

    # overridable hooks for 404-on-delete tests; default nothing is missing
    def deleted_missing_relationships(self):
        return set()

    def deleted_missing_objects(self):
        return set()


class ApplyCliTestCase(unittest.TestCase):
    def setUp(self):
        self.core = FakeCore()
        self.addCleanup(self.core.stop)

    def run_cli(self, extra_args):
        args = [
            "--model", MODEL_PATH,
            "--core", self.core.base_url,
            "--email", "admin@chawpi.local",
            "--password", "admin",
        ] + extra_args
        out = io.StringIO()
        err = io.StringIO()
        with redirect_stdout(out), redirect_stderr(err):
            code = apply.main(args)
        return code, out.getvalue(), err.getvalue()


class DryRunTests(ApplyCliTestCase):
    def test_dry_run_makes_no_requests_and_prints_headers(self):
        code, out, err = self.run_cli(["--dry-run"])
        self.assertEqual(code, 0)
        self.assertEqual(self.core.requests, [])
        self.assertEqual(out.count("# POST /api/objects"), 13)
        self.assertEqual(out.count("# POST /api/relationships"), 17)


class HappyPathTests(ApplyCliTestCase):
    def test_creates_everything_in_order(self):
        code, out, err = self.run_cli([])
        self.assertEqual(code, 0, msg=err)

        methods_paths = [(m, p) for (m, p, a, b) in self.core.requests]
        logins = [r for r in methods_paths if r == ("POST", "/api/auth/login")]
        self.assertEqual(len(logins), 1)

        gets = [r for r in methods_paths if r[0] == "GET"]
        self.assertEqual(gets, [("GET", "/api/objects")])

        object_posts = [r for r in self.core.requests if r[1] == "/api/objects" and r[0] == "POST"]
        self.assertEqual(len(object_posts), 13)
        self.assertEqual(object_posts[0][3]["name"], "sector")
        self.assertEqual(object_posts[-1][3]["name"], "padron")

        rel_posts = [r for r in self.core.requests if r[1] == "/api/relationships" and r[0] == "POST"]
        self.assertEqual(len(rel_posts), 17)

        puts = [r for r in self.core.requests if r[0] == "PUT"]
        self.assertEqual(len(puts), 1)
        self.assertEqual(puts[0][1], "/api/metadata/objects/valoracion/fields/frente_arancelario")
        self.assertEqual(puts[0][3], {"required": True})

        # Authorization present on everything but the login call.
        for method, path, auth, body in self.core.requests:
            if path == "/api/auth/login":
                self.assertIsNone(auth)
            else:
                self.assertEqual(auth, "Bearer t")

        self.assertIn("done: 30 created, 0 skipped", out)


class IdempotencyTests(ApplyCliTestCase):
    def setUp(self):
        with open(MODEL_PATH) as f:
            model = json.load(f)
        object_names = [o["name"] for o in model["objects"]]
        rel_names = [r["name"] for r in model["relationships"]]
        self.core = FakeCore(existing_objects=object_names, existing_relationships=rel_names)
        self.addCleanup(self.core.stop)

    def test_everything_skips(self):
        code, out, err = self.run_cli([])
        self.assertEqual(code, 0, msg=err)
        object_posts = [r for r in self.core.requests if r[1] == "/api/objects" and r[0] == "POST"]
        self.assertEqual(len(object_posts), 0)
        skip_lines = [l for l in out.splitlines() if l.startswith("skip")]
        self.assertEqual(len(skip_lines), 13 + 17)
        self.assertIn("done: 0 created, 30 skipped", out)


class FailureStopsTests(ApplyCliTestCase):
    def setUp(self):
        self.core = FakeCore(fail_on_post_object="via")  # third object in file order
        self.addCleanup(self.core.stop)

    def test_500_on_third_object_aborts(self):
        code, out, err = self.run_cli([])
        self.assertEqual(code, 1)
        self.assertIn("boom", err)
        self.assertIn("/api/objects", err)
        object_posts = [r for r in self.core.requests if r[1] == "/api/objects" and r[0] == "POST"]
        # sector, unidad_urbana created, via fails -> exactly 3 attempts, no more.
        self.assertEqual(len(object_posts), 3)
        rel_posts = [r for r in self.core.requests if r[1] == "/api/relationships"]
        self.assertEqual(rel_posts, [])


class RequiredPutFailureTests(ApplyCliTestCase):
    def setUp(self):
        self.core = FakeCore(fail_put=True)
        self.addCleanup(self.core.stop)

    def test_500_on_required_put_aborts(self):
        code, out, err = self.run_cli([])
        self.assertEqual(code, 1)
        self.assertIn("/api/metadata/objects/valoracion/fields/frente_arancelario", err)
        self.assertIn("boom-put", err)

        # valoracion_frente_arancelario is the 15th of 17 relationships (file order);
        # the PUT that follows its POST fails, so parque_sector/padron_predio must not be posted.
        rel_posts = [r[3]["name"] for r in self.core.requests if r[1] == "/api/relationships" and r[0] == "POST"]
        self.assertEqual(rel_posts[-1], "valoracion_frente_arancelario")
        self.assertNotIn("parque_sector", rel_posts)
        self.assertNotIn("padron_predio", rel_posts)

        puts = [r for r in self.core.requests if r[0] == "PUT"]
        self.assertEqual(len(puts), 1)


class RequiredPutConflictFailureTests(ApplyCliTestCase):
    """The required PUT tolerates nothing, not even 409 — unlike object/relationship POSTs,
    which skip on 409 ('ya existe')."""

    def setUp(self):
        self.core = FakeCore(fail_put=True, fail_put_status=409)
        self.addCleanup(self.core.stop)

    def test_409_on_required_put_aborts(self):
        code, out, err = self.run_cli([])
        self.assertEqual(code, 1)
        self.assertIn("/api/metadata/objects/valoracion/fields/frente_arancelario", err)
        self.assertIn("boom-put", err)
        self.assertNotIn("warning", err)

        rel_posts = [r[3]["name"] for r in self.core.requests if r[1] == "/api/relationships" and r[0] == "POST"]
        self.assertEqual(rel_posts[-1], "valoracion_frente_arancelario")
        self.assertNotIn("parque_sector", rel_posts)
        self.assertNotIn("padron_predio", rel_posts)


class ConnectionFailureTests(unittest.TestCase):
    """A closed port must be a clean fatal error, not an uncaught traceback."""

    def test_closed_port_is_fatal_with_no_traceback(self):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.bind(("127.0.0.1", 0))
        port = s.getsockname()[1]
        s.close()  # port is now closed; nothing listens on it

        out = io.StringIO()
        err = io.StringIO()
        args = [
            "--model", MODEL_PATH,
            "--core", f"http://127.0.0.1:{port}",
            "--email", "admin@chawpi.local",
            "--password", "admin",
        ]
        with redirect_stdout(out), redirect_stderr(err):
            code = apply.main(args)

        self.assertEqual(code, 1)
        self.assertIn("connection failed", err.getvalue())
        self.assertNotIn("Traceback", err.getvalue())


class LoginMissingTokenTests(ApplyCliTestCase):
    def setUp(self):
        self.core = FakeCore(login_response={})
        self.addCleanup(self.core.stop)

    def test_login_without_token_is_fatal(self):
        code, out, err = self.run_cli([])
        self.assertEqual(code, 1)
        self.assertIn("error POST /api/auth/login -> no token in response", err)
        # nothing past login should have been attempted.
        self.assertEqual(len(self.core.requests), 1)


class DropTests(ApplyCliTestCase):
    def test_drop_deletes_relationships_then_objects_in_reverse(self):
        code, out, err = self.run_cli(["--drop"])
        self.assertEqual(code, 0, msg=err)

        rel_deletes = [r for r in self.core.requests if r[0] == "DELETE" and r[1].startswith("/api/relationships/")]
        self.assertEqual(len(rel_deletes), 17)
        self.assertEqual(rel_deletes[0][1], "/api/relationships/padron_predio")
        self.assertEqual(rel_deletes[-1][1], "/api/relationships/manzana_catastral_sector")

        obj_deletes = [r for r in self.core.requests if r[0] == "DELETE" and r[1].startswith("/api/objects/")]
        self.assertEqual(len(obj_deletes), 13)
        self.assertEqual(obj_deletes[0][1], "/api/objects/padron")
        self.assertEqual(obj_deletes[-1][1], "/api/objects/sector")

        # relationship deletes happen entirely before object deletes.
        all_deletes = [r for r in self.core.requests if r[0] == "DELETE"]
        last_rel_index = max(i for i, r in enumerate(all_deletes) if r[1].startswith("/api/relationships/"))
        first_obj_index = min(i for i, r in enumerate(all_deletes) if r[1].startswith("/api/objects/"))
        self.assertLess(last_rel_index, first_obj_index)

        self.assertIn("done: 30 deleted, 0 skipped", out)

    def test_drop_dry_run_makes_no_requests(self):
        code, out, err = self.run_cli(["--drop", "--dry-run"])
        self.assertEqual(code, 0, msg=err)
        self.assertEqual(self.core.requests, [])
        self.assertEqual(out.count("# DELETE /api/relationships/"), 17)
        self.assertEqual(out.count("# DELETE /api/objects/"), 13)


class ValidateOnlyTests(ApplyCliTestCase):
    def test_validate_only_makes_no_requests(self):
        code, out, err = self.run_cli(["--validate-only"])
        self.assertEqual(code, 0, msg=err)
        self.assertEqual(self.core.requests, [])


if __name__ == "__main__":
    unittest.main()
