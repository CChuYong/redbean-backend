package co.bearus.magcloud.domain.service.diary

import co.bearus.magcloud.advice.SHA256
import co.bearus.magcloud.domain.exception.DomainException
import co.bearus.magcloud.domain.exception.NotFoundException
import co.bearus.magcloud.domain.entity.diary.UserDiaryEntity
import co.bearus.magcloud.domain.entity.user.UserEntity
import co.bearus.magcloud.domain.repository.JPAUserDiaryRepository
import co.bearus.magcloud.domain.repository.JPAUserRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class UserDiaryService(
    private val userRepository: JPAUserRepository,
    private val diaryRepository: JPAUserDiaryRepository,
  //  private val inferenceService: InferenceService
) {
    fun addDiary(
        userId: Long,
        diaryDate: String,
        content: String
    ): co.bearus.magcloud.controller.dto.response.APIResponse {
        val user = findUser(userId)

        val date = LocalDate.parse(diaryDate, DateTimeFormatter.BASIC_ISO_DATE)
        val previousDiaries = diaryRepository.getByUserIdAndDate(userId, date)
        if (previousDiaries != null) throw DomainException("이미 해당 날짜에 일기가 존재합니다")

        val userDiary = UserDiaryEntity(
            content,
            user,
            date
        )
        val requestedResult = diaryRepository.save(userDiary)
      //  inferenceService.requestInference(requestedResult)
        return co.bearus.magcloud.controller.dto.response.APIResponse.ok("일기가 추가되었습니다")
    }

    fun patchDiary(
        userId: Long,
        dto: co.bearus.magcloud.controller.dto.request.DiaryPatchDTO
    ): co.bearus.magcloud.controller.dto.response.APIResponse {
        val date = LocalDate.parse(dto.date, DateTimeFormatter.BASIC_ISO_DATE)
        val previousDiaries =
            diaryRepository.getByUserIdAndDate(userId, date) ?: throw DomainException("일기가 존재하지 않습니다")


        previousDiaries.content = dto.content
        previousDiaries.contentHash = SHA256.encrypt(dto.content)


        diaryRepository.save(previousDiaries)
    //    inferenceService.requestInference(previousDiaries)
        return co.bearus.magcloud.controller.dto.response.APIResponse.ok("일기가 수정되었습니다.");
    }

    fun getDiariesOfUser(userId: Long): List<co.bearus.magcloud.controller.dto.response.DiaryResponseDTO> {
        val user = findUser(userId)
        return user.diaries.map {
            co.bearus.magcloud.controller.dto.response.DiaryResponseDTO(
                it.id!!,
                it.content,
                it.date.atStartOfDay(),
                it.modifiedDate!!,
                it.emotions.map { emotion ->
                    co.bearus.magcloud.controller.dto.response.EmotionResponseDTO(
                        emotion.emotion,
                        emotion.value
                    )
                }
            )
        }
    }

    fun updateRequest(
        userId: Long,
        payload: List<co.bearus.magcloud.controller.dto.request.UpdateRequestDTO>
    ): List<co.bearus.magcloud.controller.dto.response.DiaryResponseDTO> {
        val user = findUser(userId)
        val dataMap = payload.associate { it.date to it.contentHash }
        return user.diaries
            .filter { !dataMap.containsKey(it.date) || dataMap[it.date] != it.contentHash }
            .map {
                co.bearus.magcloud.controller.dto.response.DiaryResponseDTO(
                    it.id!!,
                    it.content,
                    it.date.atStartOfDay(),
                    it.modifiedDate!!,
                    it.emotions.map { emotion ->
                        co.bearus.magcloud.controller.dto.response.EmotionResponseDTO(
                            emotion.emotion,
                            emotion.value
                        )
                    }
                )
            }
    }

    fun getDiaryByDate(userId: Long, date: LocalDate): co.bearus.magcloud.controller.dto.response.DiaryResponseDTO? {
        val diary = diaryRepository.getByUserIdAndDate(userId, date)
            ?: throw NotFoundException()
        return co.bearus.magcloud.controller.dto.response.DiaryResponseDTO(
            diary.id!!,
            diary.content,
            diary.date.atStartOfDay(),
            diary.modifiedDate!!,
            diary.emotions.map { emotion ->
                co.bearus.magcloud.controller.dto.response.EmotionResponseDTO(
                    emotion.emotion,
                    emotion.value
                )
            })
    }

    private fun findUser(userId: Long): UserEntity {
        val user = userRepository.findById(userId)
        if (!user.isPresent) throw NotFoundException("그런 유저는 찾을 수 없습니다")
        return user.get()
    }
}

