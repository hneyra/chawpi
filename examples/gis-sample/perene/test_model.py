"""Tests for validate(), object_payload() and relationship_payload() in apply.py.

Run: cd examples/gis-sample/perene && python3 -m unittest -v
"""
import copy
import json
import os
import unittest

from apply import validate, object_payload, relationship_payload

MODEL_PATH = os.path.join(os.path.dirname(__file__), "model.json")


def load_model():
    with open(MODEL_PATH) as f:
        return json.load(f)


class RealModelTests(unittest.TestCase):
    def setUp(self):
        self.model = load_model()

    def test_shipped_model_validates_clean(self):
        errors = validate(self.model)
        self.assertEqual(errors, [])

    def test_object_and_relationship_counts(self):
        self.assertEqual(len(self.model["objects"]), 13)
        self.assertEqual(len(self.model["relationships"]), 17)

    def test_geometry_object_count(self):
        geom_objects = [
            o for o in self.model["objects"]
            if any(f["type"] == "GEOMETRY" for f in o["fields"])
        ]
        self.assertEqual(len(geom_objects), 11)

    def test_every_geometry_payload_uses_model_srid(self):
        for obj in self.model["objects"]:
            payload = object_payload(self.model, obj)
            for field in payload["fields"]:
                if field["type"] == "GEOMETRY":
                    self.assertEqual(field["srid"], 32718)
                    self.assertEqual(field["dimension"], 2)


class ObjectPayloadTests(unittest.TestCase):
    def setUp(self):
        self.model = load_model()

    def test_sector_payload_matches_expected_shape(self):
        obj = next(o for o in self.model["objects"] if o["name"] == "sector")
        payload = object_payload(self.model, obj)
        self.assertEqual(payload["name"], "sector")
        self.assertEqual(payload["label"], "Sector")
        self.assertEqual(payload["pluralLabel"], "Sectores")
        self.assertEqual(
            payload["description"],
            "Sector catastral del distrito, unidad territorial base de la nomenclatura.",
        )
        self.assertEqual(len(payload["fields"]), 4)

        id_gdb = payload["fields"][0]
        self.assertEqual(id_gdb, {
            "name": "id_gdb", "label": "ID GDB", "type": "INTEGER",
            "required": False, "unique": True,
        })

        codigo = payload["fields"][1]
        self.assertEqual(codigo, {
            "name": "codigo", "label": "Código", "type": "TEXT",
            "required": True, "unique": True,
        })

        fuente = payload["fields"][2]
        self.assertEqual(fuente["type"], "ENUM")
        self.assertEqual(fuente["enumOptions"], self.model["enums"]["fuente"])
        self.assertNotIn("enum", fuente)
        self.assertNotIn("source", fuente)

        geom = payload["fields"][3]
        self.assertEqual(geom["geometryType"], "MULTIPOLYGON")
        self.assertEqual(geom["srid"], 32718)
        self.assertEqual(geom["dimension"], 2)
        self.assertNotIn("source", geom)

    def test_no_field_ever_carries_source_or_enum_keys(self):
        for obj in self.model["objects"]:
            payload = object_payload(self.model, obj)
            for field in payload["fields"]:
                self.assertNotIn("source", field)
                self.assertNotIn("enum", field)


class RelationshipPayloadTests(unittest.TestCase):
    def setUp(self):
        self.model = load_model()

    def test_relationship_payload_shape(self):
        rel = next(
            r for r in self.model["relationships"]
            if r["name"] == "valoracion_frente_arancelario"
        )
        payload = relationship_payload(self.model, rel)
        self.assertEqual(payload, {
            "name": "valoracion_frente_arancelario",
            "label": "Frente arancelario",
            "inverseLabel": "Valoraciones arancelarias",
            "type": "MANY_TO_ONE",
            "source": "valoracion",
            "target": "frente_arancelario",
            "fieldName": "frente_arancelario",
        })
        self.assertNotIn("required", payload)


def base_model():
    """A minimal, valid model to mutate per test."""
    return {
        "name": "t", "description": "t", "srid": 4326,
        "enums": {"color": ["RED", "GREEN"]},
        "objects": [
            {
                "name": "target_obj", "label": "Target", "pluralLabel": "Targets",
                "description": "d", "source": "S",
                "fields": [
                    {"name": "codigo", "label": "Codigo", "type": "TEXT"},
                ],
            },
            {
                "name": "source_obj", "label": "Source", "pluralLabel": "Sources",
                "description": "d", "source": "S",
                "fields": [
                    {"name": "nombre", "label": "Nombre", "type": "TEXT"},
                ],
            },
        ],
        "relationships": [
            {
                "name": "source_obj_target_obj", "label": "L", "inverseLabel": "IL",
                "source": "source_obj", "target": "target_obj",
                "fieldName": "target_obj", "required": False,
            },
        ],
    }


class SyntheticValidationTests(unittest.TestCase):
    def test_valid_base_model_has_no_errors(self):
        self.assertEqual(validate(base_model()), [])

    def test_object_name_too_long_is_rejected(self):
        model = base_model()
        model["objects"][0]["name"] = "a" * 40
        model["relationships"][0]["target"] = "a" * 40
        errors = validate(model)
        self.assertTrue(any("a" * 40 in e for e in errors))

    def test_field_named_id_is_rejected(self):
        model = base_model()
        model["objects"][0]["fields"].append({"name": "id", "label": "Id", "type": "TEXT"})
        errors = validate(model)
        self.assertTrue(any("id" in e and "target_obj" in e for e in errors))

    def test_sql_keyword_field_name_is_rejected(self):
        model = base_model()
        model["objects"][0]["fields"].append({"name": "select", "label": "Select", "type": "TEXT"})
        errors = validate(model)
        self.assertTrue(any("select" in e for e in errors))

    def test_enum_option_with_slash_is_rejected(self):
        model = base_model()
        model["enums"]["color"].append("BLUE/GREEN")
        errors = validate(model)
        self.assertTrue(any("BLUE/GREEN" in e for e in errors))

    def test_unknown_enum_is_rejected(self):
        model = base_model()
        model["objects"][0]["fields"].append(
            {"name": "tono", "label": "Tono", "type": "ENUM", "enum": "does_not_exist"}
        )
        errors = validate(model)
        self.assertTrue(any("does_not_exist" in e for e in errors))

    def test_relation_field_type_is_rejected(self):
        model = base_model()
        model["objects"][0]["fields"].append(
            {"name": "otro", "label": "Otro", "type": "RELATION"}
        )
        errors = validate(model)
        self.assertTrue(any("RELATION" in e for e in errors))

    def test_relationship_target_after_source_is_rejected(self):
        model = base_model()
        # swap order so target_obj (index 0) now comes after source_obj... invert instead:
        model["objects"] = list(reversed(model["objects"]))
        errors = validate(model)
        self.assertIn(
            "relationship source_obj_target_obj: target 'target_obj' must appear before "
            "source 'source_obj' in objects",
            errors,
        )

    def test_geometry_field_unique_is_rejected(self):
        model = base_model()
        model["objects"][0]["fields"].append(
            {"name": "geom", "label": "Geom", "type": "GEOMETRY",
             "geometryType": "POINT", "unique": True}
        )
        errors = validate(model)
        self.assertTrue(any("geom" in e and "unique" in e.lower() for e in errors))

    def test_relationship_field_name_too_long_is_rejected(self):
        model = base_model()
        long_name = "n" * 50
        model["relationships"][0]["fieldName"] = long_name
        errors = validate(model)
        self.assertTrue(any(long_name in e and "exceeds" in e for e in errors))

    def test_relationship_field_name_bad_shape_is_rejected(self):
        model = base_model()
        model["relationships"][0]["fieldName"] = "Target-Obj"
        errors = validate(model)
        self.assertTrue(any("Target-Obj" in e for e in errors))

    def test_relationship_field_name_reserved_is_rejected(self):
        model = base_model()
        model["relationships"][0]["fieldName"] = "id"
        errors = validate(model)
        self.assertTrue(any("fieldName 'id'" in e and "reserved" in e.lower() for e in errors))

    def test_duplicate_source_field_name_pair_is_rejected(self):
        model = base_model()
        model["objects"].insert(0, {
            "name": "other_target", "label": "Other Target", "pluralLabel": "Other Targets",
            "description": "d",
            "fields": [{"name": "codigo", "label": "Codigo", "type": "TEXT"}],
        })
        model["relationships"].append({
            "name": "source_obj_other_target", "label": "L2", "inverseLabel": "IL2",
            "source": "source_obj", "target": "other_target",
            "fieldName": "target_obj", "required": False,
        })
        errors = validate(model)
        self.assertTrue(
            any("target_obj" in e and "duplicate" in e.lower() for e in errors)
        )


class EnumOptionRegexTests(unittest.TestCase):
    """Mirror Core's ENUM regex exactly: ^[\\p{L}0-9 _.-]{1,64}$."""

    def test_superscript_digit_is_rejected(self):
        model = base_model()
        model["enums"]["color"].append("m²")  # m²
        errors = validate(model)
        self.assertTrue(any("m²" in e and "invalid" in e.lower() for e in errors))

    def test_arabic_indic_digit_is_rejected(self):
        model = base_model()
        model["enums"]["color"].append("٣")  # Arabic-Indic digit three
        errors = validate(model)
        self.assertTrue(any("٣" in e and "invalid" in e.lower() for e in errors))

    def test_unicode_letters_space_and_punctuation_are_accepted(self):
        model = base_model()
        model["enums"]["color"].append("Añó 1_.-")  # "Añó 1_.-"
        errors = validate(model)
        self.assertEqual(errors, [])


FIELD_REQUEST_KEYS = {
    "name", "label", "type", "required", "unique", "defaultValue", "description",
    "enumOptions", "relationTarget", "geometryType", "srid", "dimension", "visible", "editable",
}
CREATE_OBJECT_REQUEST_KEYS = {"name", "label", "pluralLabel", "description", "fields"}
CREATE_RELATIONSHIP_REQUEST_KEYS = {"name", "label", "inverseLabel", "type", "source", "target", "fieldName"}


class PayloadKeysMatchCoreDtosTests(unittest.TestCase):
    """apply.py's payloads must send only keys Core's DTOs (FieldRequest, CreateObjectRequest,
    CreateRelationshipRequest) actually declare."""

    def setUp(self):
        self.model = load_model()

    def test_object_payload_keys_are_subset_of_create_object_request(self):
        for obj in self.model["objects"]:
            payload = object_payload(self.model, obj)
            self.assertTrue(
                set(payload.keys()) <= CREATE_OBJECT_REQUEST_KEYS,
                f"{obj['name']}: {set(payload.keys()) - CREATE_OBJECT_REQUEST_KEYS}",
            )
            for field in payload["fields"]:
                self.assertTrue(
                    set(field.keys()) <= FIELD_REQUEST_KEYS,
                    f"{obj['name']}.{field.get('name')}: {set(field.keys()) - FIELD_REQUEST_KEYS}",
                )

    def test_relationship_payload_keys_are_subset_of_create_relationship_request(self):
        for rel in self.model["relationships"]:
            payload = relationship_payload(self.model, rel)
            self.assertTrue(
                set(payload.keys()) <= CREATE_RELATIONSHIP_REQUEST_KEYS,
                f"{rel['name']}: {set(payload.keys()) - CREATE_RELATIONSHIP_REQUEST_KEYS}",
            )


if __name__ == "__main__":
    unittest.main()
