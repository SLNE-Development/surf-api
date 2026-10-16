package dev.slne.surf.api.minestom.server.impl.command

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.StringArgumentType
import dev.slne.surf.api.minestom.command.argument.*
import dev.slne.surf.api.minestom.command.suggestion.ArgumentSuggestions
import dev.slne.surf.api.minestom.command.suggestion.SafeSuggestions
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.coordinate.Vec
import net.minestom.server.utils.Range
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

@EnvTest
class ArgumentDefinitionTest {
    @Test
    fun `numeric and suggestion configuration freezes into a definition`() {
        val argument = IntegerArgument("amount", 1, 64)
            .setOptional(4)
            .withPermission(" lobby.amount ")
            .replaceSuggestions(ArgumentSuggestions.strings("1", "4", "16"))

        val definition = argument.toDefinition()

        assertEquals(ArgumentKind.Integer(min = 1, max = 64), definition.kind)
        assertTrue(definition.optional)
        assertEquals(setOf("lobby.amount"), definition.permissions)
        assertInstanceOf(SuggestionMode.Replace::class.java, definition.suggestions)
    }

    @Test
    fun `configuration methods retain the fluent receiver`() {
        val argument = IntegerArgument("amount")
        val suggestions = ArgumentSuggestions.strings("1")
        val safeSuggestions = SafeSuggestions.suggest(1)

        assertSame(argument, argument.setOptional(true))
        assertSame(argument, argument.setOptional(1))
        assertSame(argument, argument.setOptional { 2 })
        assertSame(argument, argument.withPermission("lobby.amount"))
        assertSame(argument, argument.withRequirement { true })
        assertSame(argument, argument.replaceSuggestions(suggestions))
        assertSame(argument, argument.includeSuggestions(suggestions))
        assertSame(argument, argument.replaceSafeSuggestions(safeSuggestions))
        assertSame(argument, argument.includeSafeSuggestions(safeSuggestions))
    }

    @Test
    fun `blank node names and permissions are rejected`() {
        assertThrows<IllegalArgumentException> { StringArgument("  ") }
        assertThrows<IllegalArgumentException> { StringArgument("value").withPermission("\t") }
    }

    @Test
    fun `numeric bounds reject reversed and non finite ranges`() {
        assertThrows<IllegalArgumentException> { IntegerArgument("value", 2, 1) }
        assertThrows<IllegalArgumentException> { LongArgument("value", 2, 1) }
        assertThrows<IllegalArgumentException> { FloatArgument("value", 2f, 1f) }
        assertThrows<IllegalArgumentException> { DoubleArgument("value", 2.0, 1.0) }
        assertThrows<IllegalArgumentException> { FloatArgument("value", Float.NaN, 1f) }
        assertThrows<IllegalArgumentException> { DoubleArgument("value", 0.0, Double.NaN) }
    }

    @Test
    fun `literal arguments reject empty literal sets and preserve their values`() {
        assertThrows<IllegalArgumentException> { LiteralArgument("choice", " ") }
        assertThrows<IllegalArgumentException> { LiteralArgument("choice", "two words") }
        assertThrows<IllegalArgumentException> { MultiLiteralArgument("choice") }
        assertThrows<IllegalArgumentException> { MultiLiteralArgument("choice", "one", "") }
        assertThrows<IllegalArgumentException> { MultiLiteralArgument("choice", "one", "two words") }
        assertThrows<IllegalArgumentException> { MultiLiteralArgument("choice", "one\ttwo") }
        assertThrows<IllegalArgumentException> { MultiLiteralArgument("choice", "ns:fast") }
        assertThrows<IllegalArgumentException> { MultiLiteralArgument("choice", "a/b") }
        assertThrows<IllegalArgumentException> { MultiLiteralArgument("choice", "ä") }
        assertEquals(
            listOf("a-b.c+d_1"),
            (MultiLiteralArgument("choice", "a-b.c+d_1").toDefinition().kind as ArgumentKind.MultiLiteral).literals,
        )

        val source = arrayOf("one", "two")
        val argument = MultiLiteralArgument("choice", *source)
        val definition = argument.toDefinition()
        source[0] = "changed"

        assertEquals(listOf("one", "two"), (definition.kind as ArgumentKind.MultiLiteral).literals)
        assertEquals("two", definition.stringify("two"))
        assertFalse(argument.javaClass.methods.any { it.name == "getLiterals" })
    }

    @Test
    fun `enum arguments use stable values and formatter`() {
        val source = mutableListOf(AccessLevel.STAFF, AccessLevel.GUEST)
        val definition = EnumArgument("access", source) { it.name.lowercase().replace("staff", "team") }
            .toDefinition()
        source.clear()

        val kind = definition.kind as ArgumentKind.Enum<AccessLevel>
        assertEquals(EnumSet.of(AccessLevel.STAFF, AccessLevel.GUEST), kind.values)
        assertEquals("team", kind.formatter(AccessLevel.STAFF))
        assertEquals("guest", definition.stringify(AccessLevel.GUEST))
    }

    @Test
    fun `enum arguments reject formatted values that Brigadier cannot read unquoted`() {
        assertThrows<IllegalArgumentException> {
            EnumArgument("access", AccessLevel.entries) { value -> "ns:${value.name.lowercase()}" }
        }
        assertThrows<IllegalArgumentException> {
            EnumArgument("access", AccessLevel.entries) { value -> "${value.name.lowercase()}/x" }
        }
    }

    @Test
    fun `uuid and range arguments expose native result kinds and command text`() {
        val uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        val uuidDefinition = UUIDArgument("id").toDefinition()
        val integerRange = IntegerRangeArgument("levels").toDefinition()
        val floatRange = FloatRangeArgument("speeds").toDefinition()

        assertEquals(ArgumentKind.Uuid, uuidDefinition.kind)
        assertEquals(uuid.toString(), uuidDefinition.stringify(uuid))
        assertEquals(ArgumentKind.IntegerRange, integerRange.kind)
        assertEquals("3..7", integerRange.stringify(Range.Int(3, 7)))
        assertEquals("..7", integerRange.stringify(Range.Int(null, 7)))
        assertEquals("3", integerRange.stringify(Range.Int(3)))
        assertEquals(ArgumentKind.FloatRange, floatRange.kind)
        assertEquals("1.5..", floatRange.stringify(Range.Float(1.5f, null)))
    }

    @Test
    fun `only the string categories that consume the rest of the line are greedy`() {
        val definitions = listOf(
            StringArgument("word").toDefinition(),
            TextArgument("text").toDefinition(),
            GreedyStringArgument("greedy").toDefinition(),
            CommandArgument("command").toDefinition(),
        )

        assertEquals(listOf(false, false, true, true), definitions.map { it.greedy })
        assertEquals(
            listOf(ArgumentKind.Word, ArgumentKind.Text, ArgumentKind.GreedyString, ArgumentKind.Command),
            definitions.map { it.kind },
        )
    }

    @Test
    fun `text argument serializes values as valid quotable phrases`(env: Env) {
        val stringify = TextArgument("text").toDefinition().stringify
        val verticalTabValue = "left\u000Bright"
        val verticalTabSerialized = stringify(verticalTabValue)

        assertEquals("plain", stringify("plain"))
        assertEquals("\"hello world\"", stringify("hello world"))
        assertEquals("\"say \\\"hello\\\"\"", stringify("say \"hello\""))
        assertEquals("\"C:\\\\temp\"", stringify("C:\\temp"))
        assertEquals("\"a:b\"", stringify("a:b"))
        assertEquals("\"héllo\"", stringify("héllo"))
        assertEquals("\"C:\\\\Program Files\\\\app\"", stringify("C:\\Program Files\\app"))
        assertEquals("\"Grüße Welt\"", stringify("Grüße Welt"))
        assertEquals("\"left\u000Bright\"", verticalTabSerialized)
        assertEquals(
            verticalTabValue,
            ArgumentType.String("text").parse(env.process().command().consoleSender, verticalTabSerialized),
        )
        listOf("plain", "a:b", "héllo", "C:\\temp", "tab\there", "new\nline", "say \"hi\"", "back\\slash").forEach { value ->
            assertEquals(value, StringArgumentType.string().parse(StringReader(stringify(value))))
        }
    }

    @Test
    fun `numeric and position kinds never serialize scientific notation`() {
        val doubles = DoubleArgument("double").toDefinition().stringify
        val floats = FloatArgument("float").toDefinition().stringify
        val floatRange = FloatRangeArgument("range").toDefinition().stringify

        listOf(1.0E-4, 1.2345678E7, -1.0E20, 12.5, 0.0).forEach { value ->
            val text = doubles(value)
            assertFalse('E' in text, text)
            assertEquals(value, text.toDouble())
        }
        listOf(1.0E-4f, 1.2345678E7f, -Float.MAX_VALUE, 12.5f).forEach { value ->
            val text = floats(value)
            assertFalse('E' in text, text)
            assertEquals(value, text.toFloat())
        }

        assertFalse('E' in floatRange(Range.Float(1.0E-4f, 1.2345678E7f)))
        assertFalse('E' in floatRange(Range.Float(-Float.MAX_VALUE, Float.MAX_VALUE)))
        assertFalse('E' in PositionArgument("pos").toDefinition().stringify(Vec(1.0E-4, 2.0, 3.0E10)))
        assertFalse('E' in Position2DArgument("pos").toDefinition().stringify(Vec(1.0E-4, 0.0, 3.0E10)))
        assertFalse('E' in RotationArgument("rot").toDefinition().stringify(Rotation(1.0E-4f, 1.2345678E7f)))
        assertFalse('E' in AngleArgument("angle").toDefinition().stringify(1.0E-4f))
        assertEquals("1.5 2.0 3.0", PositionArgument("pos").toDefinition().stringify(Vec(1.5, 2.0, 3.0)))
    }

    @Test
    fun `primitive kinds retain their exact bounds`() {
        assertEquals(ArgumentKind.Boolean, BooleanArgument("flag").toDefinition().kind)
        assertEquals(ArgumentKind.Long(1, 2), LongArgument("long", 1, 2).toDefinition().kind)
        assertEquals(ArgumentKind.Float(1f, 2f), FloatArgument("float", 1f, 2f).toDefinition().kind)
        assertEquals(ArgumentKind.Double(1.0, 2.0), DoubleArgument("double", 1.0, 2.0).toDefinition().kind)
        assertEquals(ArgumentKind.Literal("confirm"), LiteralArgument("action", "confirm").toDefinition().kind)
    }

    @Test
    fun `optional defaults retain constant and sender aware suppliers`(env: Env) {
        val sender = env.process().command().consoleSender
        val constant = IntegerArgument("constant").setOptional(4).toDefinition()
        val supplier: (net.minestom.server.command.CommandSender) -> Int = { if (it === sender) 8 else 0 }
        val supplied = IntegerArgument("supplied").setOptional(supplier).toDefinition()
        val absent = IntegerArgument("absent").setOptional(true).toDefinition()
        val required = IntegerArgument("required").setOptional(false).toDefinition()

        assertTrue(constant.optional)
        assertEquals(4, constant.defaultValue?.invoke(sender))
        assertTrue(supplied.optional)
        assertSame(supplier, supplied.defaultValue)
        assertEquals(8, supplied.defaultValue?.invoke(sender))
        assertTrue(absent.optional)
        assertNull(absent.defaultValue)
        assertFalse(required.optional)
        assertNull(required.defaultValue)
    }

    @Test
    fun `named boolean optional overloads distinguish defaults from optionality`(env: Env) {
        val sender = env.process().command().consoleSender
        val falseDefault = BooleanArgument("false-default").setOptional(default = false).toDefinition()
        val trueDefault = BooleanArgument("true-default").setOptional(default = true).toDefinition()
        val required = BooleanArgument("required").setOptional(optional = false).toDefinition()
        val optional = BooleanArgument("optional").setOptional(optional = true).toDefinition()

        assertTrue(falseDefault.optional)
        assertEquals(false, falseDefault.defaultValue?.invoke(sender))
        assertTrue(trueDefault.optional)
        assertEquals(true, trueDefault.defaultValue?.invoke(sender))
        assertFalse(required.optional)
        assertNull(required.defaultValue)
        assertTrue(optional.optional)
        assertNull(optional.defaultValue)
    }

    @Test
    fun `snapshots isolate mutable builder state and preserve requirement order`(env: Env) {
        val calls = mutableListOf<String>()
        val firstRequirement: (net.minestom.server.command.CommandSender) -> Boolean = {
            calls += "first"
            true
        }
        val secondRequirement: (net.minestom.server.command.CommandSender) -> Boolean = {
            calls += "second"
            true
        }
        val argument = StringArgument("target")
            .withPermission("lobby.first")
            .withRequirement(firstRequirement)
        val snapshot = argument.toDefinition()

        argument
            .withPermission("lobby.second")
            .withRequirement(secondRequirement)
            .includeSuggestions(ArgumentSuggestions.strings("later"))

        assertEquals(setOf("lobby.first"), snapshot.permissions)
        assertEquals(listOf(firstRequirement), snapshot.requirements)
        snapshot.requirements.forEach { it(env.process().command().consoleSender) }
        assertEquals(listOf("first"), calls)
        assertEquals(SuggestionMode.BuiltIns, snapshot.suggestions)
        assertThrows<UnsupportedOperationException> {
            (snapshot.permissions as MutableSet<String>).add("mutated")
        }
        assertThrows<UnsupportedOperationException> {
            (snapshot.requirements as MutableList<(net.minestom.server.command.CommandSender) -> Boolean>).clear()
        }
    }

    @Test
    fun `all suggestion modes remain explicit in snapshots`() {
        val strings = ArgumentSuggestions.strings("one")
        val safe = SafeSuggestions.suggest(1)

        assertEquals(SuggestionMode.BuiltIns, IntegerArgument("built-in").toDefinition().suggestions)
        assertSame(
            strings,
            (IntegerArgument("replace").replaceSuggestions(strings).toDefinition().suggestions as SuggestionMode.Replace).provider,
        )
        assertSame(
            strings,
            (IntegerArgument("include").includeSuggestions(strings).toDefinition().suggestions as SuggestionMode.Include).provider,
        )
        assertSame(
            safe,
            (IntegerArgument("replace-safe").replaceSafeSuggestions(safe).toDefinition().suggestions as SuggestionMode.ReplaceSafe).provider,
        )
        assertSame(
            safe,
            (IntegerArgument("include-safe").includeSafeSuggestions(safe).toDefinition().suggestions as SuggestionMode.IncludeSafe).provider,
        )
    }

    private enum class AccessLevel {
        STAFF,
        GUEST,
    }
}
