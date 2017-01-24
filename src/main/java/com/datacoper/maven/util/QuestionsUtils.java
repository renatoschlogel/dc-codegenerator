/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.datacoper.maven.util;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.datacoper.maven.enums.options.IOptions;
import com.datacoper.maven.exception.DcRuntimeException;
import com.datacoper.maven.exception.OperationCanceledByUser;
import com.datacoper.maven.mojos.MojoConstants;

/**
 *
 * @author alessandro
 */
public final class QuestionsUtils {
    
    private static boolean lastBreakIsGroup = true;
    
    private QuestionsUtils() { }
    
    public  static void sysOutEndHelper() {
        ConsoleUtil.sysOut("####  Informe {0} para interromper  ####\n", MojoConstants.STOP_INFORMATION_PARAMS);
        lastBreakIsGroup = false;
    }

    public static void sysOutSeparatorHelper() {
        if (!lastBreakIsGroup) {
            ConsoleUtil.sysOut("\n------------------------------------------------------------------------\n\n");
        }
        
        lastBreakIsGroup = false;
    }

    public static void sysOutSeparatorHelper(String message, Object... params) {
        lastBreakIsGroup = true;
        
        message = message.trim();

        message = MessageFormat.format(message, params);

        message = StringUtil.completeWith(message, 60, 1, "#");

        ConsoleUtil.sysOut("\n######{0}######\n\n", message);
    }
    
    public static Optional<String> question(String question, Object... params) {
        return question(Converters::toString, question, params);
    }
    
    public static <T> Optional<T> question(Function<String, T> converter, String question, Object... params) {
        Optional<T> answer = Optional.empty();
        try {
            answer = question(converter, question, false, false, params);
        } catch (OperationCanceledByUser ex) {}
        
        return answer;
    }
    
    public static String questionNonEmpty(String question, Object... params) {
        String answer = "";
        try {
            answer = questionNonEmpty(Converters::toString, question, params);
        } catch (OperationCanceledByUser ex) { }
        
        return answer;
    }
    
    public static <T> T questionNonEmpty(Function<String, T> converter, String question, Object... params) throws OperationCanceledByUser {
        return question(converter, question, true, false, params).get();
    }
    
    //***
    public static Optional<String> questionInGroup(String question, Object... params) throws OperationCanceledByUser {
        return questionInGroup(Converters::toString, question, params);
    }
    
    public static <T> Optional<T> questionInGroup(Function<String, T> converter, String question, Object... params) throws OperationCanceledByUser {
        return question(converter, question, false, true, params);
    }
    
    public static String questionInGroupNonEmpty(String question, Object... params) throws OperationCanceledByUser {
        return questionInGroupNonEmpty(Converters::toString, question, params).get();
    }
    
    public static <T> Optional<T> questionInGroupNonEmpty(Function<String, T> converter, String question, Object... params) throws OperationCanceledByUser {
        return question(converter, question, true, true, params);
    }
    
    //****
    
    public static <T> Optional<T> question(Function<String, T> converter, String question, boolean required, boolean inGroup, Object... params) throws OperationCanceledByUser {
        
        while(true) {
            sysOutSeparatorHelper();
            
            String answer = ConsoleUtil.question(question, params);
            if (inGroup && MojoConstants.isStopInformationParams(answer)) throw new OperationCanceledByUser();
            
            try {
                if (required && answer.isEmpty()) throw new DcRuntimeException("Not accept empty value");
                
                return Optional.ofNullable(converter.apply(answer));
            } catch (Throwable e) {
                LogUtil.error(e);
            }
        }
    }
    
    public static Set<String> questionGroupSingleQuestion(String groupName, String question, Object... params) {
        return questionGroupSingleQuestion(Converters::toString, groupName, question, params);
    }
    
    public static <T> Set<T> questionGroupSingleQuestion(Function<String, T> converter, String groupName, String question, Object... params) {
        Supplier<Optional<T>> questionMethod = () -> QuestionsUtils.questionForGroup(converter, question, true, params);
        
        return questionGroup(groupName, questionMethod);
    }
    
    /**
     * Se retornar Optional.empty, o grupo para de ser executado
     * @param <T>
     * @param groupName
     * @param groupQuestionMethod
     * @return 
    */
    public static <T> Set<T> questionGroup(String groupName, Supplier<Optional<T>> groupQuestionMethod) {
        sysOutSeparatorHelper("START {0}", groupName);

        sysOutEndHelper();
        
        Set<T> values = new HashSet<>();

        while (true) {
            try {
                Optional<T> value = groupQuestionMethod.get();
                
                if (!value.isPresent()) break;
                
                values.add(value.get());
                
            } catch (Throwable e) {
                if (e instanceof OperationCanceledByUser) break;
                LogUtil.error(e);
            }
        }

        sysOutSeparatorHelper("FINISH  {0}", groupName);
        
        return values;
    }
    
    public static <K, V> Map<String, String> questionMapValues(String groupName, Object... params) {
        groupName = StringUtil.format(groupName, params);
        
        return questionMapValues(groupName, Converters::toString, Converters::toString);
    }
    
    public static <K, V> Map<K, V> questionMapValues(String groupName, Function<String, K> convertKey, Function<String, V> conrvertValue) {
        sysOutSeparatorHelper("START PARAMS FOR {0}", groupName);
        
        Map<K, V> map= new HashMap<>();
        
        try {
            while(true) {
                String key = questionInGroupNonEmpty("Report property name: ");

                try {
                    String value = questionInGroupNonEmpty("Report value for property {0}", key);
                    
                    map.put(convertKey.apply(key), conrvertValue.apply(value));
                } catch (OperationCanceledByUser ex) {
                    LogUtil.warn("the prorty {0} wasn't add.", key);
                }
            }
        } catch (OperationCanceledByUser ex) {
            sysOutSeparatorHelper("END PARAMS FOR {0}", groupName);
            
            return map;
        }
    }
    
    public static <T extends IOptions> Optional<T> questionParamter(String question, Class<T> options, Object... params) throws OperationCanceledByUser {
        return questionWithParamter(question, options, false, false, params);
    }
    
    public static <T extends IOptions> T questionParamterNonEmpty(String question, Class<T> options, Object... params) throws OperationCanceledByUser {
        return questionWithParamter(question, options, true, false, params).get();
    }
    
    public static <T extends IOptions> Optional<T> questionInGroupParamter(String question, Class<T> options, Object... params) throws OperationCanceledByUser {
        return questionWithParamter(question, options, false, true, params);
    }
    
    public static <T extends IOptions> Optional<T> questionIngroupParamterNonEmpty(String question, Class<T> options, Object... params) throws OperationCanceledByUser {
        return questionWithParamter(question, options, true, true, params);
    }
    
    @SuppressWarnings("unchecked")
	private static <T extends IOptions> Optional<T> questionWithParamter(String question, Class<T> options, boolean required, boolean inGroup, Object... params) throws OperationCanceledByUser {
        IOptions[] enumConstants = options.getEnumConstants();
        IOptions enums = enumConstants[0];
        
        while(true) {
            sysOutSeparatorHelper();
            
            question = enums.print().concat(question);
            
            Optional<String> answer = question(Converters::toString, question, required, inGroup, params);
            
            return Optional.ofNullable((T) enums.of(answer.get()));
        }
    }

    private static <T> Optional<T> questionForGroup(Function<String, T> converter, String question, boolean required, Object... params) {
        return question(converter, question, required, params);
    }
}
