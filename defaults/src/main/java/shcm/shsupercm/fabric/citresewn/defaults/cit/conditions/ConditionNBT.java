package shcm.shsupercm.fabric.citresewn.defaults.cit.conditions;

import com.google.gson.JsonParser;
import io.shcm.shsupercm.fabric.fletchingtable.api.Entrypoint;
import com.mojang.brigadier.StringReader;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import shcm.shsupercm.fabric.citresewn.api.CITConditionContainer;
import shcm.shsupercm.fabric.citresewn.cit.CITCondition;
import shcm.shsupercm.fabric.citresewn.cit.CITContext;
import shcm.shsupercm.fabric.citresewn.cit.CITParsingException;
import shcm.shsupercm.fabric.citresewn.pack.format.PropertyGroup;
import shcm.shsupercm.fabric.citresewn.pack.format.PropertyKey;
import shcm.shsupercm.fabric.citresewn.pack.format.PropertyValue;

import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ConditionNBT extends CITCondition {
    /*? <1.21 {*//*@Entrypoint(CITConditionContainer.ENTRYPOINT)*//*?}*/
    public static final CITConditionContainer<ConditionNBT> CONTAINER = new CITConditionContainer<>(ConditionNBT.class, ConditionNBT::new,
            "nbt");

    protected String[] path;

    protected StringMatcher matchString = null;
    protected IntTag matchInteger = null;
    protected ByteTag matchByte = null;
    protected FloatTag matchFloat = null;
    protected DoubleTag matchDouble = null;
    protected LongTag matchLong = null;
    protected ShortTag matchShort = null;
    protected Tag matchElement = null;

    @Override
    public void load(PropertyKey key, PropertyValue value, PropertyGroup properties) throws CITParsingException {
        if (value.keyMetadata() == null || value.keyMetadata().isEmpty())
            throw new CITParsingException("Missing nbt path", properties, value.position());

        String[] nbtPath = value.keyMetadata().split("\\.");
        for (String s : nbtPath)
            if (s.isEmpty())
                throw new CITParsingException("Path segment cannot be empty", properties, value.position());

        loadNbtCondition(value, properties, nbtPath, value.value());
    }

    public void loadNbtCondition(PropertyValue value, PropertyGroup properties, String[] path, String nbtValue) throws CITParsingException {
        this.path = path;

        try {
            if (nbtValue.startsWith("regex:"))
                matchString = StringMatcher.regex(nbtValue.substring(6));
            else if (nbtValue.startsWith("iregex:"))
                matchString = StringMatcher.iregex(nbtValue.substring(7));
            else if (nbtValue.startsWith("pattern:"))
                matchString = new StringMatcher.PatternMatcher(nbtValue.substring(8));
            else if (nbtValue.startsWith("ipattern:"))
                matchString = new StringMatcher.IPatternMatcher(nbtValue.substring(9));
            else
                matchString = new StringMatcher.DirectMatcher(nbtValue);
        } catch (PatternSyntaxException e) {
            throw new CITParsingException("Malformatted regex expression", properties, value.position(), e);
        } catch (Exception ignored) { }
        try {
            if (nbtValue.startsWith("#"))
                matchInteger = IntTag.valueOf(Integer.parseInt(nbtValue.substring(1).toLowerCase(Locale.ENGLISH), 16));
            else if (nbtValue.startsWith("0x"))
                matchInteger = IntTag.valueOf(Integer.parseInt(nbtValue.substring(2).toLowerCase(Locale.ENGLISH), 16));
            else
                matchInteger = IntTag.valueOf(Integer.parseInt(nbtValue));
        } catch (Exception ignored) { }
        try {
            matchByte = ByteTag.valueOf(Byte.parseByte(nbtValue));
        } catch (Exception ignored) { }
        try {
            matchFloat = FloatTag.valueOf(Float.parseFloat(nbtValue));
        } catch (Exception ignored) { }
        try {
            matchDouble = DoubleTag.valueOf(Double.parseDouble(nbtValue));
        } catch (Exception ignored) { }
        try {
            matchLong = LongTag.valueOf(Long.parseLong(nbtValue));
        } catch (Exception ignored) { }
        try {
            matchShort = ShortTag.valueOf(Short.parseShort(nbtValue));
        } catch (Exception ignored) { }
        try {
            matchElement = TagParser.create(NbtOps.INSTANCE).parseFully(nbtValue);
        } catch (Exception ignored) { }
    }

    @Override
    public boolean test(CITContext context) {
        /*? >=1.21 {*/
        throw new AssertionError("NBT condition replaced with component condition in 1.21");
        /*?} else {*/
        /*return testPath(context.stack.getNbt(), 0, context);
        *//*?}*/
    }

    public boolean testPath(Tag element, int pathIndex, CITContext context) {
        if (element == null)
            return false;

        if (pathIndex >= path.length)
            return testValue(element, context);

        final String path = this.path[pathIndex];
        if (path.equals("*")) {
            if (element instanceof CompoundTag compound) {
                for (Tag subElement : compound.values())
                    if (testPath(subElement, pathIndex + 1, context))
                        return true;
            } else if (element instanceof ListTag list) {
                for (Tag subElement : list)
                    if (testPath(subElement, pathIndex + 1, context))
                        return true;
            }
        } else {
            if (element instanceof CompoundTag compound)
                return testPath(compound.get(path), pathIndex + 1, context);
            else if (element instanceof ListTag list) {
                if (path.equals("count"))
                    return testValue(IntTag.valueOf(list.size()), context);

                try {
                    return testPath(list.get(Integer.parseInt(path)), pathIndex + 1, context);
                } catch (NumberFormatException | IndexOutOfBoundsException ignored) { }
            }
        }

        return false;
    }

    public boolean testValue(Tag element, CITContext context) {
        try {
            if (element instanceof StringTag nbtString)
                return testString(nbtString.value(), null, context);
            else if (element instanceof IntTag nbtInt && matchInteger != null)
                return nbtInt.equals(matchInteger);
            else if (element instanceof ByteTag nbtByte && matchByte != null)
                return nbtByte.equals(matchByte);
            else if (element instanceof FloatTag nbtFloat && matchFloat != null)
                return nbtFloat.equals(matchFloat);
            else if (element instanceof DoubleTag nbtDouble && matchDouble != null)
                return nbtDouble.equals(matchDouble);
            else if (element instanceof LongTag nbtLong && matchLong != null)
                return nbtLong.equals(matchLong);
            else if (element instanceof ShortTag nbtShort && matchShort != null)
                return nbtShort.equals(matchShort);
            else if ((element instanceof CompoundTag || element instanceof ListTag) && matchElement != null)
                return NbtUtils.compareNbt(matchElement, element, true);

            if (element instanceof NumericTag nbtNumber && !(matchString instanceof StringMatcher.DirectMatcher))
                return matchString.matches(String.valueOf(nbtNumber.box()));
        } catch (Exception ignored) { }
        return false;
    }

    public boolean testString(String element, Component elementText, CITContext context) {
        if (element != null) {
            if (matchString.matches(element))
                return true;

            if (elementText == null && looksLikeJson(element)) {
                try {
                    elementText = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(element)).result().orElse(null);
                } catch (Exception ignored) { }
            }
        }

        if (elementText == null)
            return false;

        return matchString.matches(elementText.getString());
    }

    private static boolean looksLikeJson(String value) {
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isWhitespace(ch))
                continue;
            return ch == '{' || ch == '[';
        }
        return false;
    }

    protected static abstract class StringMatcher {
        public abstract boolean matches(String value);

        public static StringMatcher regex(String pattern) {
            String literal = literalRegex(pattern);
            return literal == null ? new RegexMatcher(pattern) : new DirectMatcher(literal);
        }

        public static StringMatcher iregex(String pattern) {
            String literal = literalRegex(pattern);
            return literal == null ? new IRegexMatcher(pattern) : new IDirectMatcher(literal);
        }

        private static String literalRegex(String pattern) {
            if (pattern.length() >= 2 && pattern.charAt(0) == '(' && pattern.charAt(pattern.length() - 1) == ')')
                pattern = pattern.substring(1, pattern.length() - 1);

            for (int i = 0; i < pattern.length(); i++) {
                char c = pattern.charAt(i);
                if (c == '\\' || c == '[' || c == ']' || c == '{' || c == '}' || c == '*' || c == '+' || c == '?' || c == '|' || c == '^' || c == '$' || c == '.')
                    return null;
            }

            return pattern;
        }

        public static class DirectMatcher extends StringMatcher {
            protected final String pattern;

            public DirectMatcher(String pattern) {
                this.pattern = pattern;
            }

            @Override
            public boolean matches(String value) {
                return pattern.equals(value);
            }
        }

        public static class IDirectMatcher extends StringMatcher {
            protected final String pattern;

            public IDirectMatcher(String pattern) {
                this.pattern = pattern;
            }

            @Override
            public boolean matches(String value) {
                return pattern.equalsIgnoreCase(value);
            }
        }

        public static class RegexMatcher extends StringMatcher {
            protected final Pattern pattern;

            public RegexMatcher(String pattern) {
                this(Pattern.compile(pattern));
            }

            protected RegexMatcher(Pattern pattern) {
                this.pattern = pattern;
            }

            @Override
            public boolean matches(String value) {
                return this.pattern.matcher(value).matches();
            }
        }

        public static class PatternMatcher extends StringMatcher {
            protected final String pattern;

            public PatternMatcher(String pattern) {
                this.pattern = pattern;
            }

            @Override
            public boolean matches(String value) {
                return matchesPattern(value, this.pattern, 0, value.length(), 0, pattern.length());
            }

            /**
             * Author: Paul "prupe" Rupe<br>
             * Taken and modified from MCPatcher under public domain licensing.<br>
             * https://bitbucket.org/prupe/mcpatcher/src/1aa45839b2cd029143809edfa60ec59e5ef75f80/newcode/src/com/prupe/mcpatcher/mal/nbt/NBTRule.java#lines-269:301
             */
            protected boolean matchesPattern(String value, String pattern, int curV, int maxV, int curG, int maxG) {
                for (; curG < maxG; curG++, curV++) {
                    char g = pattern.charAt(curG);
                    if (g == '*') {
                        while (true) {
                            if (matchesPattern(value, pattern, curV, maxV, curG + 1, maxG)) {
                                return true;
                            }
                            if (curV >= maxV) {
                                break;
                            }
                            curV++;
                        }
                        return false;
                    } else if (curV >= maxV) {
                        break;
                    } else if (g == '?') {
                        continue;
                    }
                    if (g == '\\' && curG + 1 < maxG) {
                        curG++;
                        g = pattern.charAt(curG);
                    }

                    if (!charsEqual(g, value.charAt(curV)))
                        return false;
                }
                return curG == maxG && curV == maxV;
            }

            protected boolean charsEqual(char p, char v) {
                return p == v;
            }
        }

        public static class IRegexMatcher extends RegexMatcher {
            public IRegexMatcher(String pattern) {
                super(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
            }
        }

        public static class IPatternMatcher extends PatternMatcher {
            public IPatternMatcher(String pattern) {
                super(pattern.toLowerCase(Locale.ROOT));
            }

            @Override
            protected boolean charsEqual(char p, char v) {
                return p == v || p == Character.toLowerCase(v);
            }
        }
    }
}
