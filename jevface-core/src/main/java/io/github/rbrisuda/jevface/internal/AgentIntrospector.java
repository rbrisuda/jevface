package io.github.rbrisuda.jevface.internal;

import io.github.rbrisuda.jevface.AgentDescriptor;
import io.github.rbrisuda.jevface.ChoiceResult;
import io.github.rbrisuda.jevface.JevDefinitionException;
import io.github.rbrisuda.jevface.JevEvaluated;
import io.github.rbrisuda.jevface.JevEvaluationException;
import io.github.rbrisuda.jevface.LowConfidenceException;
import io.github.rbrisuda.jevface.NoulResult;
import io.github.rbrisuda.jevface.QuestionDescriptor;
import io.github.rbrisuda.jevface.ScoreResult;
import io.github.rbrisuda.jevface.annotation.ChoiceQuestion;
import io.github.rbrisuda.jevface.annotation.JevAgent;
import io.github.rbrisuda.jevface.annotation.NoulQuestion;
import io.github.rbrisuda.jevface.annotation.Option;
import io.github.rbrisuda.jevface.annotation.ScoreQuestion;
import io.github.rbrisuda.jevface.spi.Answer;
import io.github.rbrisuda.jevface.spi.ChoiceAnswer;
import io.github.rbrisuda.jevface.spi.ChoiceSpec;
import io.github.rbrisuda.jevface.spi.NoulAnswer;
import io.github.rbrisuda.jevface.spi.NoulSpec;
import io.github.rbrisuda.jevface.spi.ScoreAnswer;
import io.github.rbrisuda.jevface.spi.ScoreSpec;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Reads a {@link JevAgent} interface, validates it and compiles it into an {@link AgentModel}.
 * All problems of an interface are collected and reported together. Internal API.
 */
public final class AgentIntrospector {

    static final String STATED_SUFFIX = "_stated";
    private static final Pattern KEY = Pattern.compile("[A-Za-z0-9_.-]+");
    private static final int MAX_CHOICE_OPTIONS = 255;
    private static final int MIN_SCORE_LEVELS = 2;
    private static final int MAX_SCORE_LEVELS = 10;

    private AgentIntrospector() {
    }

    public static <T> AgentModel<T> introspect(Class<T> type) {
        if (!type.isInterface() || type.isAnnotation()) {
            throw new JevDefinitionException(type, List.of("must be an interface annotated with @JevAgent"));
        }
        JevAgent agent = type.getAnnotation(JevAgent.class);
        if (agent == null) {
            throw new JevDefinitionException(type, List.of("is missing the @JevAgent annotation"));
        }
        List<String> problems = new ArrayList<>();
        if (type.getTypeParameters().length > 0) {
            problems.add("generic agent interfaces are not supported");
        }
        if (!isUnitInterval(agent.minConfidence())) {
            problems.add("@JevAgent.minConfidence must be between 0 and 1");
        }

        Method[] methods = type.getMethods();
        Arrays.sort(methods, Comparator.comparing(Method::getName));
        List<QuestionBinding> bindings = new ArrayList<>();
        Map<String, Method> keys = new HashMap<>();
        for (Method method : methods) {
            if (!isQuestionCandidate(method)) {
                continue;
            }
            try {
                QuestionBinding binding = bind(method, agent.minConfidence());
                QuestionDescriptor question = binding.descriptor();
                claimKey(keys, question.key(), method);
                String statedKey = question.statedKey();
                if (statedKey != null) {
                    claimKey(keys, statedKey, method);
                }
                bindings.add(binding);
            } catch (InvalidQuestion e) {
                problems.add(method.getName() + "(): " + e.getMessage());
            }
        }
        if (bindings.isEmpty() && problems.isEmpty()) {
            problems.add("declares no question methods");
        }
        if (!problems.isEmpty()) {
            throw new JevDefinitionException(type, problems);
        }

        bindings.sort(Comparator.comparing(binding -> binding.descriptor().key()));
        String name = agent.name().isBlank() ? type.getSimpleName() : agent.name();
        AgentDescriptor descriptor = new AgentDescriptor(type, name, agent.description(), blankToNull(agent.model()),
                bindings.stream().map(QuestionBinding::descriptor).toList());
        return new AgentModel<>(type, descriptor, bindings);
    }

    static boolean isObjectMethod(Method method) {
        return switch (method.getName()) {
            case "toString", "hashCode" -> method.getParameterCount() == 0;
            case "equals" -> method.getParameterCount() == 1 && method.getParameterTypes()[0] == Object.class;
            default -> false;
        };
    }

    private static boolean isQuestionCandidate(Method method) {
        return !method.isDefault()
                && !Modifier.isStatic(method.getModifiers())
                && method.getDeclaringClass() != JevEvaluated.class
                && !isObjectMethod(method);
    }

    private static void claimKey(Map<String, Method> keys, String key, Method method) {
        Method previous = keys.putIfAbsent(key, method);
        if (previous != null) {
            throw new InvalidQuestion("question id '" + key + "' is already used by " + previous.getName() + "()");
        }
    }

    private static QuestionBinding bind(Method method, double agentMinConfidence) {
        if (method.getParameterCount() > 0) {
            throw new InvalidQuestion("question methods take no parameters; the prompt is the only input");
        }
        if (method.getTypeParameters().length > 0) {
            throw new InvalidQuestion("question methods cannot be generic");
        }
        NoulQuestion noul = method.getAnnotation(NoulQuestion.class);
        ChoiceQuestion choice = method.getAnnotation(ChoiceQuestion.class);
        ScoreQuestion score = method.getAnnotation(ScoreQuestion.class);
        int annotations = (noul != null ? 1 : 0) + (choice != null ? 1 : 0) + (score != null ? 1 : 0);
        if (annotations == 0) {
            throw new InvalidQuestion("is abstract but has no @NoulQuestion, @ChoiceQuestion or @ScoreQuestion; "
                    + "annotate it, or make it a default method");
        }
        if (annotations > 1) {
            throw new InvalidQuestion("has more than one question annotation");
        }
        ReturnType returnType = ReturnType.of(method.getGenericReturnType());
        if (noul != null) {
            return bindNoul(method, noul, returnType);
        }
        if (choice != null) {
            return bindChoice(method, choice, returnType, agentMinConfidence);
        }
        return bindScore(method, requireNonNull(score), returnType, agentMinConfidence);
    }

    // ---------------------------------------------------------------- noul

    private enum NoulKind { BOOLEAN, DOUBLE, RESULT }

    private static QuestionBinding bindNoul(Method method, NoulQuestion question, ReturnType returnType) {
        String key = key(question.key(), method);
        String instructions = instructions(question.value());
        double threshold = question.threshold();
        if (!isUnitInterval(threshold)) {
            throw new InvalidQuestion("threshold must be between 0 and 1");
        }
        if (returnType.optional()) {
            throw new InvalidQuestion("@NoulQuestion cannot return Optional: a noul always has an answer");
        }
        Class<?> raw = returnType.rawInner();
        NoulKind kind;
        if (raw == boolean.class || raw == Boolean.class) {
            kind = NoulKind.BOOLEAN;
        } else if (raw == double.class || raw == Double.class) {
            kind = NoulKind.DOUBLE;
        } else if (raw == NoulResult.class) {
            kind = NoulKind.RESULT;
        } else {
            throw new InvalidQuestion("@NoulQuestion cannot return " + returnType.inner().getTypeName()
                    + "; use boolean, double or NoulResult");
        }
        NoulSpec spec = new NoulSpec(instructions, blankToNull(question.whenTrue()), blankToNull(question.whenFalse()));
        QuestionDescriptor descriptor = new QuestionDescriptor(key, method, spec, null, null, 0.0, Map.of());
        return new QuestionBinding(descriptor, (answer, stated) -> {
            double value = expect(answer, NoulAnswer.class, key).value();
            return Outcome.of(switch (kind) {
                case BOOLEAN -> value >= threshold;
                case DOUBLE -> value;
                case RESULT -> new NoulResult(value, threshold);
            });
        });
    }

    // ---------------------------------------------------------------- choice

    private static QuestionBinding bindChoice(
            Method method, ChoiceQuestion question, ReturnType returnType, double agentMinConfidence) {
        String key = key(question.key(), method);
        String instructions = instructions(question.value());
        boolean result = returnType.rawInner() == ChoiceResult.class;
        Class<?> valueType = result ? returnType.typeArgument(ChoiceResult.class) : returnType.rawInner();

        Map<String, Object> options = new LinkedHashMap<>();
        List<ChoiceSpec.Option> specOptions = new ArrayList<>();
        if (valueType.isEnum()) {
            if (question.options().length > 0) {
                throw new InvalidQuestion("options come from enum " + valueType.getSimpleName() + "; remove options()");
            }
            for (Enum<?> constant : enumConstants(valueType)) {
                Option option = optionOf(constant);
                String label = option != null && !option.label().isBlank()
                        ? option.label()
                        : constant.name().toLowerCase(Locale.ROOT);
                addOption(options, label, constant);
                specOptions.add(new ChoiceSpec.Option(label, option != null ? blankToNull(option.description()) : null));
            }
        } else if (valueType == String.class) {
            if (question.options().length == 0) {
                throw new InvalidQuestion("a String choice needs options = {@Option(label = ...), ...}, "
                        + "or return an enum instead");
            }
            for (Option option : question.options()) {
                if (option.label().isBlank()) {
                    throw new InvalidQuestion("every @Option in options() needs a label");
                }
                addOption(options, option.label(), option.label());
                specOptions.add(new ChoiceSpec.Option(option.label(), blankToNull(option.description())));
            }
        } else {
            throw new InvalidQuestion("@ChoiceQuestion cannot return " + returnType.declared().getTypeName()
                    + "; use an enum, String or ChoiceResult<...>, optionally wrapped in Optional");
        }
        if (options.size() < 2) {
            throw new InvalidQuestion("a choice needs at least 2 options");
        }
        if (options.size() > MAX_CHOICE_OPTIONS) {
            throw new InvalidQuestion("a choice supports at most " + MAX_CHOICE_OPTIONS + " options");
        }

        Gate gate = gate(key, question.stated(), question.statedThreshold(), question.minConfidence(),
                agentMinConfidence, returnType);
        QuestionDescriptor descriptor = new QuestionDescriptor(key, method,
                new ChoiceSpec(instructions, specOptions), gate.statedKey(), gate.statedSpec(), gate.minConfidence(),
                options);
        return new QuestionBinding(descriptor, (answer, stated) -> {
            ChoiceAnswer choice = expect(answer, ChoiceAnswer.class, key);
            Object value = options.get(choice.choice());
            if (value == null) {
                throw new JevEvaluationException("Jev answered '" + choice.choice() + "' for question '" + key
                        + "', which is not one of " + options.keySet());
            }
            Map<Object, Double> probabilities = new LinkedHashMap<>();
            options.forEach((label, option) -> probabilities.put(option, choice.probabilities().getOrDefault(label, 0.0)));
            Object exposed = result ? new ChoiceResult<>(value, choice.confidence(), probabilities) : value;
            return gate.apply(exposed, result, choice.confidence(), stated);
        });
    }

    // ---------------------------------------------------------------- score

    private enum ScoreKind { DOUBLE, INDEX, LEVEL, RESULT }

    private static QuestionBinding bindScore(
            Method method, ScoreQuestion question, ReturnType returnType, double agentMinConfidence) {
        String key = key(question.key(), method);
        String instructions = instructions(question.value());
        Class<?> raw = returnType.rawInner();
        ScoreKind kind;
        @Nullable Class<?> levelType;
        if (raw == ScoreResult.class) {
            kind = ScoreKind.RESULT;
            levelType = returnType.typeArgument(ScoreResult.class);
        } else if (raw == double.class || raw == Double.class) {
            kind = ScoreKind.DOUBLE;
            levelType = null;
        } else if (raw == int.class || raw == Integer.class) {
            kind = ScoreKind.INDEX;
            levelType = null;
        } else if (raw.isEnum() || raw == String.class) {
            kind = ScoreKind.LEVEL;
            levelType = raw;
        } else {
            throw new InvalidQuestion("@ScoreQuestion cannot return " + returnType.declared().getTypeName()
                    + "; use double, int, an enum, String or ScoreResult<...>, optionally wrapped in Optional");
        }

        List<Object> levels = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        if (levelType != null && levelType.isEnum()) {
            if (question.levels().length > 0) {
                throw new InvalidQuestion("levels come from enum " + levelType.getSimpleName() + "; remove levels()");
            }
            for (Enum<?> constant : enumConstants(levelType)) {
                Option option = optionOf(constant);
                String text = option == null ? constant.name().toLowerCase(Locale.ROOT)
                        : !option.description().isBlank() ? option.description()
                        : !option.label().isBlank() ? option.label()
                        : constant.name().toLowerCase(Locale.ROOT);
                levels.add(constant);
                texts.add(text);
            }
        } else if (levelType == null || levelType == String.class) {
            if (question.levels().length == 0) {
                throw new InvalidQuestion("needs levels = {...}, or return an enum / ScoreResult<enum> instead");
            }
            for (String level : question.levels()) {
                if (level.isBlank()) {
                    throw new InvalidQuestion("levels must not be blank");
                }
                levels.add(level);
                texts.add(level);
            }
        } else {
            throw new InvalidQuestion("ScoreResult levels must be an enum or String, not " + levelType.getSimpleName());
        }
        if (levels.size() < MIN_SCORE_LEVELS || levels.size() > MAX_SCORE_LEVELS) {
            throw new InvalidQuestion("a score needs between " + MIN_SCORE_LEVELS + " and " + MAX_SCORE_LEVELS
                    + " levels, found " + levels.size());
        }
        if (texts.stream().distinct().count() != texts.size()) {
            throw new InvalidQuestion("score levels must be distinct");
        }

        Map<String, Object> options = new LinkedHashMap<>();
        for (int i = 0; i < levels.size(); i++) {
            options.put(texts.get(i), levels.get(i));
        }
        Gate gate = gate(key, question.stated(), question.statedThreshold(), question.minConfidence(),
                agentMinConfidence, returnType);
        QuestionDescriptor descriptor = new QuestionDescriptor(key, method, new ScoreSpec(instructions, texts),
                gate.statedKey(), gate.statedSpec(), gate.minConfidence(), options);
        return new QuestionBinding(descriptor, (answer, stated) -> {
            ScoreAnswer score = expect(answer, ScoreAnswer.class, key);
            int index = (int) Math.max(0, Math.min(levels.size() - 1, Math.round(score.score())));
            Object level = levels.get(index);
            Object exposed = switch (kind) {
                case DOUBLE -> score.score();
                case INDEX -> index;
                case LEVEL -> level;
                case RESULT -> {
                    Map<Object, Double> probabilities = new LinkedHashMap<>();
                    for (int i = 0; i < levels.size(); i++) {
                        probabilities.put(levels.get(i), score.probabilities().getOrDefault(i, 0.0));
                    }
                    yield new ScoreResult<>(score.score(), level, index, score.confidence(), probabilities);
                }
            };
            return gate.apply(exposed, kind == ScoreKind.RESULT, score.confidence(), stated);
        });
    }

    // ---------------------------------------------------------------- confidence and stated gates

    /** Applies the {@code stated} gate and the confidence floor. */
    private record Gate(
            String key,
            boolean optional,
            @Nullable String statedKey,
            @Nullable NoulSpec statedSpec,
            double statedThreshold,
            double minConfidence) {

        Outcome apply(Object exposed, boolean isResultType, double confidence, @Nullable NoulAnswer stated) {
            if (optional) {
                boolean absent = (stated != null && stated.value() < statedThreshold) || confidence < minConfidence;
                return Outcome.of(absent ? Optional.empty() : Optional.of(exposed));
            }
            if (!isResultType && confidence < minConfidence) {
                return Outcome.failure(() -> new LowConfidenceException(key, confidence, minConfidence));
            }
            return Outcome.of(exposed);
        }
    }

    private static Gate gate(String key, String stated, double statedThreshold, double questionMinConfidence,
            double agentMinConfidence, ReturnType returnType) {
        double minConfidence = questionMinConfidence >= 0 ? questionMinConfidence : agentMinConfidence;
        if (!isUnitInterval(minConfidence)) {
            throw new InvalidQuestion("minConfidence must be between 0 and 1");
        }
        if (stated.isBlank()) {
            return new Gate(key, returnType.optional(), null, null, statedThreshold, minConfidence);
        }
        if (!returnType.optional()) {
            throw new InvalidQuestion("stated(...) means the answer can be absent, so the return type must be Optional<"
                    + returnType.inner().getTypeName() + ">");
        }
        if (!isUnitInterval(statedThreshold)) {
            throw new InvalidQuestion("statedThreshold must be between 0 and 1");
        }
        return new Gate(key, true, key + STATED_SUFFIX, NoulSpec.of(stated), statedThreshold, minConfidence);
    }

    // ---------------------------------------------------------------- helpers

    /** Declared return type, split into an optional {@code Optional<...>} wrapper and the inner type. */
    private record ReturnType(Type declared, boolean optional, Type inner) {

        static ReturnType of(Type declared) {
            if (declared == Optional.class) {
                throw new InvalidQuestion("raw Optional is not supported; declare Optional<...>");
            }
            if (declared instanceof ParameterizedType parameterized && parameterized.getRawType() == Optional.class) {
                return new ReturnType(declared, true, parameterized.getActualTypeArguments()[0]);
            }
            return new ReturnType(declared, false, declared);
        }

        Class<?> rawInner() {
            if (inner instanceof Class<?> type) {
                return type;
            }
            if (inner instanceof ParameterizedType parameterized && parameterized.getRawType() instanceof Class<?> type) {
                return type;
            }
            throw new InvalidQuestion("unsupported return type " + declared.getTypeName());
        }

        Class<?> typeArgument(Class<?> wrapper) {
            if (inner instanceof ParameterizedType parameterized
                    && parameterized.getActualTypeArguments()[0] instanceof Class<?> argument) {
                return argument;
            }
            throw new InvalidQuestion("declare " + wrapper.getSimpleName() + "<YourEnum> or "
                    + wrapper.getSimpleName() + "<String>, not " + declared.getTypeName());
        }
    }

    static String defaultKey(String methodName) {
        String name = methodName;
        if (name.startsWith("get") && name.length() > 3 && Character.isUpperCase(name.charAt(3))) {
            name = name.substring(3);
        }
        StringBuilder key = new StringBuilder(name.length() + 4);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                boolean afterLower = i > 0 && (Character.isLowerCase(name.charAt(i - 1)) || Character.isDigit(name.charAt(i - 1)));
                boolean endOfAcronym = i > 0 && Character.isUpperCase(name.charAt(i - 1))
                        && i + 1 < name.length() && Character.isLowerCase(name.charAt(i + 1));
                if (afterLower || endOfAcronym) {
                    key.append('_');
                }
                key.append(Character.toLowerCase(c));
            } else {
                key.append(c);
            }
        }
        return key.toString();
    }

    private static String key(String explicitKey, Method method) {
        String key = explicitKey.isBlank() ? defaultKey(method.getName()) : explicitKey;
        if (!KEY.matcher(key).matches()) {
            throw new InvalidQuestion("question id '" + key + "' may only contain letters, digits, '_', '-' and '.'");
        }
        return key;
    }

    private static String instructions(String value) {
        if (value.isBlank()) {
            throw new InvalidQuestion("the question text must not be blank");
        }
        return value;
    }

    private static void addOption(Map<String, Object> options, String label, Object value) {
        if (options.putIfAbsent(label, value) != null) {
            throw new InvalidQuestion("duplicate option label '" + label + "'");
        }
    }

    private static List<Enum<?>> enumConstants(Class<?> enumType) {
        Object[] constants = enumType.getEnumConstants();
        List<Enum<?>> result = new ArrayList<>(constants.length);
        for (Object constant : constants) {
            result.add((Enum<?>) constant);
        }
        return result;
    }

    private static @Nullable Option optionOf(Enum<?> constant) {
        try {
            return constant.getDeclaringClass().getField(constant.name()).getAnnotation(Option.class);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private static <A extends Answer> A expect(Answer answer, Class<A> type, String key) {
        if (!type.isInstance(answer)) {
            throw new JevEvaluationException("Expected a " + type.getSimpleName() + " for question '" + key
                    + "' but got " + answer.getClass().getSimpleName());
        }
        return type.cast(answer);
    }

    private static boolean isUnitInterval(double value) {
        return value >= 0.0 && value <= 1.0;
    }

    private static @Nullable String blankToNull(String value) {
        return value.isBlank() ? null : value;
    }

    private static <T> T requireNonNull(@Nullable T value) {
        if (value == null) {
            throw new IllegalStateException("unexpected null");
        }
        return value;
    }

    /** A problem with one question method; collected into a {@link JevDefinitionException}. */
    private static final class InvalidQuestion extends RuntimeException {
        InvalidQuestion(String message) {
            super(message, null, false, false);
        }
    }
}
