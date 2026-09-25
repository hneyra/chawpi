package chawpi.core.autoconfigure

import chawpi.core.admin.AdminService
import chawpi.core.admin.RoleAdminController
import chawpi.core.admin.UserAdminController
import chawpi.core.identity.CurrentUser
import chawpi.core.metadata.CustomObjectRepository
import chawpi.core.metadata.MetadataService
import chawpi.core.metadata.ObjectSchemaManager
import chawpi.core.organization.OrganizationController
import chawpi.core.organization.OrganizationRepository
import chawpi.core.organization.OrganizationService
import chawpi.core.platform.ChawpiSchemas
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.security.crypto.password.PasswordEncoder

// users, roles, permissions and the tenant itself
@AutoConfiguration(after = [ChawpiDataAutoConfiguration::class])
class ChawpiAdminAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun adminService(
        db: DatabaseClient,
        metadata: MetadataService,
        passwordEncoder: PasswordEncoder,
        currentUser: CurrentUser,
        schemas: ChawpiSchemas
    ): AdminService = AdminService(db, metadata, passwordEncoder, currentUser, schemas)

    @Bean
    @ConditionalOnMissingBean
    fun userAdminController(admin: AdminService): UserAdminController = UserAdminController(admin)

    @Bean
    @ConditionalOnMissingBean
    fun roleAdminController(admin: AdminService): RoleAdminController = RoleAdminController(admin)

    @Bean
    @ConditionalOnMissingBean
    fun organizationRepository(
        db: DatabaseClient,
        schemas: ChawpiSchemas
    ): OrganizationRepository = OrganizationRepository(db, schemas)

    @Bean
    @ConditionalOnMissingBean
    fun organizationService(
        organizations: OrganizationRepository,
        objects: CustomObjectRepository,
        schema: ObjectSchemaManager,
        passwordEncoder: PasswordEncoder,
        currentUser: CurrentUser,
        db: DatabaseClient,
        schemas: ChawpiSchemas
    ): OrganizationService = OrganizationService(organizations, objects, schema, passwordEncoder, currentUser, db, schemas)

    @Bean
    @ConditionalOnMissingBean
    fun organizationController(organizations: OrganizationService): OrganizationController = OrganizationController(organizations)
}
