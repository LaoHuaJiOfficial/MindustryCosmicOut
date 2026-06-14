package mod.extend.sector;

import java.util.Arrays;

public class FlowStat {
    private static final int valueWindow = 60;

    public transient float counter;
    public float mean;
    public transient boolean loaded;

    private final transient float[] window = new float[valueWindow];
    private transient int index;
    private transient int count;

    public void tickCounter(float amount) {
        counter += amount;
    }

    public void flushCounter() {
        if (!loaded && mean > 0f) {
            Arrays.fill(window, mean);
            count = valueWindow;
            loaded = true;
        }

        window[index] = Math.max(counter, 0f);
        index = (index + 1) % valueWindow;
        if (count < valueWindow) count++;

        counter = 0f;
        mean = average();
    }

    public void tickDelta(float delta) {
        if (!loaded && mean > 0f) {
            Arrays.fill(window, mean);
            count = valueWindow;
            loaded = true;
        }

        window[index] = delta;
        index = (index + 1) % valueWindow;
        if (count < valueWindow) count++;
        mean = average();
    }

    private float average() {
        if (count == 0) return 0f;
        float sum = 0f;
        for (int i = 0; i < count; i++) sum += window[i];
        return sum / count;
    }
}
