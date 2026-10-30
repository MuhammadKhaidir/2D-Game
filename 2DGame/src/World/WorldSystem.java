package World;

public class WorldSystem {

    // TIME
    public int minute = 12;
    public int hour = 6;
    public int day = 1;
    private int secondCounter = 0;

    // WEATHER
    public boolean isRaining = false;
    private int rainTimer = 0;
    private final int RAIN_DURATION = 1000; // 30 menit game

    public int getTargetDarkness() {

        // SIANG TERANG
        if (hour >= 6 && hour < 17) {
            return 0;
        }

        // SENJA (17 - 18)
        if (hour >= 17 && hour < 18) {
            return 60;
        }

        // MALAM (18 - 22)
        if (hour >= 18 && hour < 22) {
            return 140;
        }

        // TENGAH MALAM (22 - 5)
        return 200;
    }

    public void update() {

        // TIME
        secondCounter+=100;
        if (secondCounter >= 60) {
            secondCounter = 0;
            minute++;

            if (minute >= 60) {
                minute = 0;
                hour++;
            }
            if (hour >= 24) {
                hour = 0;
                day++;
            }
        }

        // HUJAN RANDOM
        rainTimer++;
        if (rainTimer >= RAIN_DURATION) {
            rainTimer = 0;
            isRaining = Math.random() < 0.5; // 50% hujan
        }
    }

    public String getTimeString() {
        return String.format("Day %d  %02d:%02d", day, hour, minute);
    }
}
