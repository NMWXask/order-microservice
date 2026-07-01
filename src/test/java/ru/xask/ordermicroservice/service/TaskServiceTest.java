package ru.xask.ordermicroservice.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.xask.ordermicroservice.dto.Person;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TaskServiceTest {

    @Test
    void onlyEvenNumbersTest() {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5);

        int result = TaskService.onlyEvenNumbers(numbers);
        int expected = 6;

        Assertions.assertEquals(result, expected);
    }

    @Test
    void getAllWordsTest() {
        List<String> words = List.of("a", "boot", "java", "is", "fun");
        List<String> expected = List.of("BOOT", "JAVA");
        List<String> result = TaskService.getAllWords(words);
        assertEquals(result, expected);
    }

    @Test
    void reverseStringTest() {
        String str = "I Love Java";
        String expected = "avaJ evoL I";
        String result = TaskService.reverseString(str);
        assertEquals(result, expected);
    }

    @Test
    void returningWordsTest() {
        String sentence = "Spring Boot is awesome";
        int expected = 4;
        int result = TaskService.returningWords(sentence);
        assertEquals(result, expected);
    }

    @Test
    void returnMaxTest() {
        List<Integer> numbers = List.of(7, 2, 99, 24);
        int expected = 99;
        int result = TaskService.returnMax(numbers);
        assertEquals(result, expected);
    }

    @Test
    void returnStrMoreThanThreeTest() {
        List<String> strings = List.of("apple", "ban", "ch", "da", "elderberry");
        List<String> expected = List.of("APPLE", "ELDERBERRY");
        List<String> result = TaskService.returnStrMoreThanThree(strings);
        assertEquals(result, expected);
    }

    @Test
    void returnStrSameIntLengthTest() {
        List<String> strings = List.of("apple", "ban", "ch", "da");
        List<Integer> expected = List.of(5, 3, 2, 2);
        List<Integer> result = TaskService.returnStrSameIntLength(strings);
        assertEquals(result, expected);
    }

    @Test
    void returnResultOfMapTest() {
        List<String> strings = List.of("apple", "banan", "che", "dat", "eld");
        Map<Integer, List<String>> expected = Map.of(5, List.of("apple", "banan"), 3, List.of("che", "dat", "eld"));
        Map<Integer, List<String>> result = TaskService.returnResultOfMap(strings);
        assertEquals(result, expected);
    }

    @Test
    void returnSortedByAgeThenByNameTest() {
        Person person1 = new Person("Alice", 30);
        Person person2 = new Person("Bob", 20);
        Person person3 = new Person("Andrey", 38);

        List<Person> persons = List.of(person1, person2, person3);

        List<Person> expected = List.of(person2, person1, person3);
        List<Person> result = TaskService.returnSortedByAgeThenByName(persons);
        assertEquals(result, expected);
    }
}
