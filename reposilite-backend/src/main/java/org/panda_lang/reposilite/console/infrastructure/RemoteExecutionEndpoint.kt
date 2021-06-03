/*
 * Copyright (c) 2021 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.panda_lang.reposilite.console.infrastructure

import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.HttpMethod
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import org.apache.http.HttpStatus
import org.panda_lang.reposilite.console.ConsoleFacade
import org.panda_lang.reposilite.console.MAX_COMMAND_LENGTH
import org.panda_lang.reposilite.console.api.ExecutionResponse
import org.panda_lang.reposilite.failure.api.ErrorResponse
import org.panda_lang.reposilite.failure.api.errorResponse
import org.panda_lang.reposilite.web.ReposiliteContextFactory
import org.panda_lang.reposilite.web.RouteHandler
import org.panda_lang.reposilite.web.RouteMethod.POST
import org.panda_lang.reposilite.web.context

internal class RemoteExecutionEndpoint(
    private val contextFactory: ReposiliteContextFactory,
    private val consoleFacade: ConsoleFacade
) : RouteHandler {

    override val route = "/api/execute"
    override val methods = listOf(POST)

    @OpenApi(
        operationId = "cli",
        method = HttpMethod.POST,
        summary = "Remote command execution",
        description = "Execute command using POST request. The commands are the same as in the console and can be listed using the 'help' command.",
        tags = ["Cli"],
        headers = [OpenApiParam(name = "Authorization", description = "Alias and token provided as basic auth credentials", required = true)],
        responses = [
            OpenApiResponse(
                status = "200",
                description = "Status of the executed command",
                content = [OpenApiContent(from = ExecutionResponse::class)]
            ),
            OpenApiResponse(
                status = "400",
                description = "Error message related to the invalid command format (0 < command length < $MAX_COMMAND_LENGTH)",
                content = [OpenApiContent(from = ErrorResponse::class)]
            ),
            OpenApiResponse(
                status = "401",
                description = "Error message related to the unauthorized access",
                content = [OpenApiContent(from = ErrorResponse::class)]
            )
        ]
    )
    override fun handle(ctx: Context) =
        context(contextFactory, ctx) {
            context.logger.info("REMOTE EXECUTION ${context.uri} from ${context.address}")

            authenticated {
                if (!isManager()) {
                    response = errorResponse(HttpStatus.SC_UNAUTHORIZED, "Authenticated user is not a manager")
                    return@authenticated
                }

                context.logger.info("${accessToken.alias} (${context.address}) requested command: ${context.body.value}")
                response = consoleFacade.executeCommand(context.body.value)
            }
        }

}