/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (6.0.1).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package sk.best.newtify.api;

import sk.best.newtify.api.dto.Error;
import sk.best.newtify.api.dto.NameDayDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Generated;

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-07-12T16:02:35.690223300+02:00[Europe/Budapest]")
@Validated
@Tag(name = "Namedays", description = "Group of endpoints to handle operations with name day")
public interface NamedaysApi {

    /**
     * GET /v1/namedays
     * Endpoint to retrieve name day for specified day and month
     *
     * @param month Refers to particular month where name day has to be checked (optional)
     * @param day Refers to particular day where name day has to be checked (optional)
     * @return Returns who has a name day at particular day (status code 200)
     *         or BadRequest (status code 400)
     *         or AccessDenied (status code 403)
     *         or InternalServerError (status code 500)
     */
    @Operation(
        operationId = "retrieveNameDay",
        tags = { "namedays" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Returns who has a name day at particular day", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = NameDayDTO.class))
            }),
            @ApiResponse(responseCode = "400", description = "BadRequest", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = Error.class))
            }),
            @ApiResponse(responseCode = "403", description = "AccessDenied", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = Error.class))
            }),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = Error.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/v1/namedays",
        produces = { "application/json" }
    )
    ResponseEntity<NameDayDTO> retrieveNameDay(
        @Parameter(name = "month", description = "Refers to particular month where name day has to be checked") @Valid @RequestParam(value = "month", required = false) Integer month,
        @Parameter(name = "day", description = "Refers to particular day where name day has to be checked") @Valid @RequestParam(value = "day", required = false) Integer day
    );

}
