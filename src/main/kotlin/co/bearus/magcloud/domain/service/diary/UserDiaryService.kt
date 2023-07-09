package co.bearus.magcloud.domain.service.diary

import co.bearus.magcloud.advice.SHA256
import co.bearus.magcloud.controller.dto.request.DiaryPatchDTO
import co.bearus.magcloud.controller.dto.response.APIResponse
import co.bearus.magcloud.controller.dto.response.DiaryIntegrityResponseDTO
import co.bearus.magcloud.controller.dto.response.DiaryResponseDTO
import co.bearus.magcloud.domain.entity.diary.DiaryEntity
import co.bearus.magcloud.domain.entity.user.UserEntity
import co.bearus.magcloud.domain.exception.*
import co.bearus.magcloud.domain.repository.JPAUserDiaryRepository
import co.bearus.magcloud.domain.repository.JPAUserRepository
import co.bearus.magcloud.domain.repository.QUserDiaryRepository
import co.bearus.magcloud.domain.type.Emotion
import co.bearus.magcloud.util.DateUtils.Companion.toEpochMillis
import co.bearus.magcloud.util.ULIDUtils
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class UserDiaryService(
    private val userRepository: JPAUserRepository,
    private val diaryRepository: JPAUserDiaryRepository,
    private val qDiaryRepository: QUserDiaryRepository,
) {
    @Transactional
    fun createDiary(
        userId: String,
        diaryDate: String,
        content: String,
        emotion: Emotion,
    ): DiaryResponseDTO {
        val user = findUser(userId)

        val date = LocalDate.parse(diaryDate, DateTimeFormatter.BASIC_ISO_DATE)
        val previousDiaries = diaryRepository.getByUserIdAndDate(userId, date)
        if (previousDiaries != null) throw IntegrityViolationException()

        val userDiary = DiaryEntity.createNewDiary(
            diaryId = ULIDUtils.generate(),
            userId = user.userId,
            emotion = emotion.name,
            date = date,
            content = content,
            contentHash = SHA256.encrypt(content),
        )
        return diaryRepository.save(userDiary).toDto()
    }

    @Transactional
    fun updateDiary(
        diaryId: String,
        userId: String,
        dto: DiaryPatchDTO,
    ): DiaryResponseDTO {
        val previousDiary = diaryRepository
            .findById(diaryId)
            .orElseThrow { DiaryNotFoundException() }

        if (previousDiary.userId != userId) throw UnAuthorizedException()

        previousDiary.updateDiary(dto)
        return diaryRepository.save(previousDiary).toDto()
    }

    fun getDiaryByDate(userId: String, date: LocalDate): DiaryResponseDTO? {
        val diary = diaryRepository.getByUserIdAndDate(userId, date)
            ?: throw DiaryNotExistsException()
        return diary.toDto()
    }

    fun getDiaryById(diaryId: String): DiaryResponseDTO {
        val diary = diaryRepository
            .findById(diaryId)
            .orElseThrow { DiaryNotExistsException() }
        return diary.toDto()
    }

    fun getDiaryIntegrityById(diaryId: String): DiaryIntegrityResponseDTO {
        val diary = qDiaryRepository.getDiaryIntegrityProjection(diaryId)
            ?: throw DiaryNotFoundException()
        return DiaryIntegrityResponseDTO(
            diaryId = diary.diaryId,
            contentHash = diary.contentHash,
            emotion = diary.emotion,
            createdAtTs = diary.createdAt.toEpochMillis(),
            updatedAtTs = diary.updatedAt.toEpochMillis(),
        )
    }

    private fun findUser(userId: String): UserEntity {
        val user = userRepository.findById(userId)
        if (!user.isPresent) throw UserNotFoundException()
        return user.get()
    }
}

