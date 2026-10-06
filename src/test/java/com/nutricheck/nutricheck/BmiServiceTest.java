package com.nutricheck.nutricheck;

import com.nutricheck.nutricheck.model.BmiCategory;
import com.nutricheck.nutricheck.model.BmiRecord;
import com.nutricheck.nutricheck.model.User;
import com.nutricheck.nutricheck.repository.BmiRecordRepository;
import com.nutricheck.nutricheck.service.BmiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.*;

class BmiServiceTest {

    private BmiService bmiService;
    private BmiRecordRepository bmiRecordRepository;

    @BeforeEach
    void setUp() {
        bmiRecordRepository = Mockito.mock(BmiRecordRepository.class);
        bmiService = new BmiService(bmiRecordRepository);
    }

    @Test
    @DisplayName("1. Calculate BMI uses standard adult formula: weight / (height^2)")
    void testCalculateBmi() {
        // Height: 175cm (1.75m), Weight: 70kg -> BMI = 70 / (1.75 * 1.75) = 22.86
        double bmi = bmiService.calculateBmi(70.0, 175.0);
        assertEquals(22.86, bmi, 0.05);
    }

    @Test
    @DisplayName("2. Age is included in calculation without altering the mathematical formula")
    void testCalculateBmiWithAge() {
        // Age is validated and accepted, but formula remains weight / (height^2)
        double bmiWithAge = bmiService.calculateBmi(70.0, 175.0, 30);
        assertEquals(22.86, bmiWithAge, 0.05);

        double bmiWithoutAge = bmiService.calculateBmi(70.0, 175.0);
        assertEquals(bmiWithAge, bmiWithoutAge, 0.001);
    }

    @Test
    @DisplayName("3. Classify BMI correctly for standard categories")
    void testClassifyBmiCategories() {
        assertEquals(BmiCategory.UNDERWEIGHT, bmiService.classifyBmi(17.5));
        assertEquals(BmiCategory.NORMAL, bmiService.classifyBmi(22.0));
        assertEquals(BmiCategory.NORMAL, bmiService.classifyBmi(24.9));
        assertEquals(BmiCategory.OVERWEIGHT, bmiService.classifyBmi(25.0));
        assertEquals(BmiCategory.OVERWEIGHT, bmiService.classifyBmi(29.5));
        assertEquals(BmiCategory.OBESE, bmiService.classifyBmi(30.0));
        assertEquals(BmiCategory.OBESE, bmiService.classifyBmi(35.2));
    }

    @Test
    @DisplayName("4. Update BMI record recalculates BMI, category, and stores updated age")
    void testUpdateBmiRecordRecalculatesAndSaves() {
        User user = new User();
        user.setId(1L);
        user.setAge(28);

        BmiRecord record = new BmiRecord(user, 175.0, 70.0, 22.86, BmiCategory.NORMAL, 28);
        record.setId(10L);

        when(bmiRecordRepository.findById(10L)).thenReturn(java.util.Optional.of(record));
        when(bmiRecordRepository.save(any(BmiRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BmiRecord updated = bmiService.updateBmiRecord(10L, 180.0, 90.0, 29, user);

        assertEquals(180.0, updated.getHeightCm());
        assertEquals(90.0, updated.getWeightKg());
        assertEquals(29, updated.getAge());
        assertEquals(27.78, updated.getBmiValue(), 0.01);
        assertEquals(BmiCategory.OVERWEIGHT, updated.getBmiCategory());
        Mockito.verify(bmiRecordRepository).save(record);
    }

    @Test
    @DisplayName("5. Update BMI record rejects another user's record")
    void testUpdateBmiRecordRejectsAnotherUsersRecord() {
        User owner = new User();
        owner.setId(1L);
        User otherUser = new User();
        otherUser.setId(2L);

        BmiRecord record = new BmiRecord(owner, 175.0, 70.0, 22.86, BmiCategory.NORMAL, 30);
        record.setId(10L);

        when(bmiRecordRepository.findById(10L)).thenReturn(java.util.Optional.of(record));

        assertThrows(SecurityException.class,
                () -> bmiService.updateBmiRecord(10L, 180.0, 90.0, 30, otherUser));
        Mockito.verify(bmiRecordRepository, Mockito.never()).save(any(BmiRecord.class));
    }

    @Test
    @DisplayName("6. Invalid height, weight, or age throws IllegalArgumentException")
    void testInvalidInputs() {
        // Height validation (< 50 or > 280)
        assertThrows(IllegalArgumentException.class, () -> bmiService.calculateBmi(70.0, 40.0));
        assertThrows(IllegalArgumentException.class, () -> bmiService.calculateBmi(70.0, 300.0));

        // Weight validation (< 20 or > 400)
        assertThrows(IllegalArgumentException.class, () -> bmiService.calculateBmi(15.0, 170.0));
        assertThrows(IllegalArgumentException.class, () -> bmiService.calculateBmi(500.0, 170.0));

        // Age validation (< 1 or > 120)
        assertThrows(IllegalArgumentException.class, () -> bmiService.calculateBmi(70.0, 175.0, 0));
        assertThrows(IllegalArgumentException.class, () -> bmiService.calculateBmi(70.0, 175.0, 130));
    }

    @Test
    @DisplayName("7. Record BMI saves measurement with user's age")
    void testRecordBmiSuccess() {
        User user = new User();
        user.setId(1L);
        user.setAge(32);

        when(bmiRecordRepository.save(any(BmiRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BmiRecord created = bmiService.recordBmi(user, 175.0, 70.0, 32);

        assertNotNull(created);
        assertEquals(175.0, created.getHeightCm());
        assertEquals(70.0, created.getWeightKg());
        assertEquals(32, created.getAge());
        assertEquals(22.86, created.getBmiValue(), 0.01);
        assertEquals(BmiCategory.NORMAL, created.getBmiCategory());
        assertEquals(user, created.getUser());
        Mockito.verify(bmiRecordRepository).save(any(BmiRecord.class));
    }

    @Test
    @DisplayName("8. Delete BMI record works for owner and rejects unauthorized user")
    void testDeleteBmiRecord() {
        User user = new User();
        user.setId(1L);
        User intruder = new User();
        intruder.setId(2L);

        BmiRecord record = new BmiRecord(user, 175.0, 70.0, 22.86, BmiCategory.NORMAL, 25);
        record.setId(5L);

        when(bmiRecordRepository.findById(5L)).thenReturn(java.util.Optional.of(record));

        // Intruder fails
        assertThrows(SecurityException.class, () -> bmiService.deleteBmiRecord(5L, intruder));
        Mockito.verify(bmiRecordRepository, Mockito.never()).delete(any(BmiRecord.class));

        // Owner succeeds
        assertDoesNotThrow(() -> bmiService.deleteBmiRecord(5L, user));
        Mockito.verify(bmiRecordRepository).delete(record);
    }
}
