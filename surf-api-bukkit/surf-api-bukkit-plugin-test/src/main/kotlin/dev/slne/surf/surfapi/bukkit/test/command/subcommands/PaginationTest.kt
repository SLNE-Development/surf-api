package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.surfapi.core.api.messages.CommonComponents
import dev.slne.surf.surfapi.core.api.messages.adventure.buildText
import dev.slne.surf.surfapi.core.api.messages.pagination.Pagination

class PaginationTest(name: String) : CommandAPICommand(name) {
    val pagination = Pagination<ExampleData> {
        title { primary("Example Pagination") }
        rowRenderer { data, index ->
            listOf(
                buildText {
                    append(CommonComponents.EM_DASH)
                    appendSpace()
                    variableValue(data.name)
                    appendSpace()
                    info("(${data.age})")
                }
            )
        }
    }

    val exampleDataList = listOf(
        ExampleData("Alice", 30),
        ExampleData("Bob", 25),
        ExampleData("Charlie", 35)
    )

    val longExampleDataList = List(100) { index ->
        ExampleData("User$index", 20 + index)
    }

    init {
        subcommand("short") {
            anyExecutor { sender, _ ->
                sender.sendMessage(pagination.renderComponent(exampleDataList))
            }
        }

        subcommand("long") {
            anyExecutor { sender, _ ->
                sender.sendMessage(pagination.renderComponent(longExampleDataList))
            }
        }
    }

    data class ExampleData(
        val name: String,
        val age: Int,
    )
}