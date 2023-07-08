package co.bearus.magcloud.domain.repository

import co.bearus.magcloud.domain.entity.user.UserEntity
import org.springframework.data.jpa.repository.JpaRepository

interface JPAUserRepository : JpaRepository<UserEntity, String>
