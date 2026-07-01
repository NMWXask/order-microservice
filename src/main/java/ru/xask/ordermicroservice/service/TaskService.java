package ru.xask.ordermicroservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.xask.ordermicroservice.dto.Person;
import ru.xask.ordermicroservice.exception.ListIsEmptyException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    public static int onlyEvenNumbers(List<Integer> numbers) {

        if (numbers == null || numbers.isEmpty()) {
            throw new ListIsEmptyException("List is empty");
        }

        int sum = 0;
        for (Integer number : numbers) {
            if (number % 2 == 0) {
                sum += number;
                log.info("Even number: {}", number);
            }
        }
        log.info("Sum: {}", sum);
        return sum;
    }

    public static List<String> getAllWords(List<String> words) {
        List<String> newWords = new ArrayList<>();
        if (words == null || words.isEmpty()) {
            log.info("List is empty");
        }
        for (String word : words) {
            if (word.length() > 3) {
                newWords.add(word.toUpperCase());
            }
        }
        log.info("Words: {}", newWords);
        return newWords;
    }


    public static String reverseString(String str) {
        if (str == null || str.isEmpty()) {
            log.info("String is Empty : {}", str);
        }
        return new StringBuilder(str).reverse().toString();
    }


    public static int returningWords(String sentence) {
        if (sentence == null || sentence.isEmpty()) {
            log.info("String id Empty: {}", sentence);
        }
        return sentence.split(" ").length;
    }

    public static int returnMax(List<Integer> numbers) {
        int max = 0;
        for (Integer number : numbers) {
            if (max < number) {
                max = number;
            }
        }
        return max;
    }

    public static List<String> returnStrMoreThanThree(List<String> strings) {
        if (strings == null || strings.isEmpty()) {
            log.info("List is empty");
        }
        return strings.stream()
                .filter(s -> s.length() > 3)
                .map(String::toUpperCase)
                .toList();
    }

    public static List<Integer> returnStrSameIntLength(List<String> strings) {
        if (strings == null || strings.isEmpty()) {
            log.info("List is empty");
        }
        return strings.stream()
                .map(String::length)
                .toList();
    }

    public static Map<Integer, List<String>> returnResultOfMap(List<String> strings) {
        if (strings == null || strings.isEmpty()) {
            log.info("List is empty");
        }
        return strings.stream()
                .collect(Collectors.groupingBy(String::length));
    }

    public static List<Person> returnSortedByAgeThenByName(List<Person> persons) {
        if (persons == null || persons.isEmpty()) {
            log.info("List of Persons is Empty");
        }
        return persons.stream()
                .sorted(Comparator.comparing(Person::age)
                        .thenComparing(Person::name))
                .collect(Collectors.toList());
    }
}

