package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;

import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.*;

/**
 * This class contains unit tests for the SecurityService class.
 * It uses Mockito framework for mocking dependencies and verifying
 * interactions.
 */
@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    private SecurityService securityService;
    private Sensor sensor;

    @Mock
    private ImageService imageService;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private StatusListener statusListener;

    /**
     * Sets up the test environment before each test.
     * Initializes the security service and a sensor.
     */
    @BeforeEach
    void setup() {
        // Initialize the security service and a sensor before each test
        securityService = new SecurityService(securityRepository, imageService);
        sensor = new Sensor(UUID.randomUUID().toString(), SensorType.DOOR);
    }

    /**
     * Generates a set of sensors with the specified properties.
     *
     * @param active a boolean indicating whether the sensors should be active or
     *               not
     * @return a set of sensors with the specified properties
     */
    private Set<Sensor> getSensors(boolean active) {
        // Helper method to generate a set of sensors
        String randomString = UUID.randomUUID().toString();
        Set<Sensor> sensors = new HashSet<>();
        IntStream.range(0, 3)
                .forEach(i -> {
                    Sensor sensor = new Sensor(randomString + "_" + i, SensorType.DOOR);
                    sensor.setActive(active);
                    sensors.add(sensor);
                });
        return sensors;
    }

    /**
     * Test 01: If alarm is armed and a sensor becomes activated, put the system
     * into pending alarm status.
     */
    @Test
    void test01() {
        // If the system is armed and a sensor is activated, the alarm status should
        // change to pending
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    /**
     * Test 02: If alarm is armed and a sensor becomes activated and the system is
     * already pending alarm, set the alarm status to alarm.
     */
    @Test
    void test02() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    /**
     * Test 03: If pending alarm and all sensors are inactive, return to no alarm
     * state.
     */
    @Test
    void test03() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /**
     * Test 04: If alarm is active, change in sensor state should not affect the
     * alarm state.
     *
     * @param status the activation status of the sensor
     */
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void test04(boolean status) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, status);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    /**
     * Test 05: If a sensor is activated while already active and the system is in
     * pending state, change it to alarm state.
     */
    @Test
    void test05() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    /**
     * Test 06: If a sensor is deactivated while already inactive, make no changes
     * to the alarm state.
     *
     * @param status the initial alarm status
     */
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = { "NO_ALARM", "PENDING_ALARM", "ALARM" })
    void test06(AlarmStatus status) {
        when(securityRepository.getAlarmStatus()).thenReturn(status);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    /**
     * Test 07: If the image service identifies an image containing a cat while the
     * system is armed-home, put the system into alarm status.
     */
    @Test
    void test07() {
        BufferedImage catImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(true);
        securityService.processImage(catImage);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    /**
     * Test 08: If the image service identifies an image that does not contain a
     * cat, change the status to no alarm as long as the sensors are not active.
     */
    @Test
    void test08() {
        Set<Sensor> sensors = getSensors(false);
        when(securityRepository.getSensors()).thenReturn(sensors);
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(false);
        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /**
     * Test 09: If the system is disarmed, set the status to no alarm.
     */
    @Test
    void test09() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /**
     * Test 10: If the system is armed, reset all sensors to inactive.
     *
     * @param status the arming status of the system
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = { "ARMED_HOME", "ARMED_AWAY" })
    void test10(ArmingStatus status) {
        Set<Sensor> sensors = getSensors(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.setArmingStatus(status);

        securityService.getSensors().forEach(sensor -> assertFalse(sensor.getActive()));
    }

    /**
     * Test 11: If the system is armed-home while the camera shows a cat, set the
     * alarm status to alarm.
     */
    @Test
    void test11() {
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(image);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }
}
