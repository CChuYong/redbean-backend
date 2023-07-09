package co.bearus.magcloud.config.filter

import co.bearus.magcloud.domain.exception.DomainException
import co.bearus.magcloud.domain.type.ContextLanguage
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

@Component
class ApplicationFilter(
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val appLanguage = request.getHeader("X-APP-LANGUAGE") ?: "KOR"
        val contextLanguage = ContextLanguage.valueOf(appLanguage)
        request.setAttribute("language", contextLanguage)
        filterChain.doFilter(request, response)
    }

//    @Throws(IOException::class)
//    private fun writeUpdateResponse(response: HttpServletResponse) {
//        response.status = HttpStatus.UNAUTHORIZED.value()
//        response.contentType = MediaType.APPLICATION_JSON_VALUE
//
//        response.outputStream.use { os ->
//            objectMapper.writeValue(os, DomainException())
//            os.flush()
//        }
//    }
}
