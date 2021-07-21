package com.material.filesystem.util;

import java.util.concurrent.TimeUnit;


/**
 * Very simple single use stopwatch for timing code blocks.
 */
public class StopWatch {
    private boolean _paused = false;
    private boolean _running = false;

    private long _startTime = 0;
    private long _lastPausedTime = 0;
    private long _pausedTotalTime = 0;

    /**
     * Returns current running time, less time that we've been paused, if any.
     *
     * @param timeUnit the {@link TimeUnit}
     * @return a double value, in the specified {@link TimeUnit}
     */
    public double currentTime(TimeUnit timeUnit) {
        long totalNanos = System.nanoTime() - _startTime - _pausedTotalTime;
        switch (timeUnit) {
            case NANOSECONDS:
                return totalNanos;
            case MICROSECONDS:
                return totalNanos / 1_000.0;
            case MILLISECONDS:
                return totalNanos / 1_000_000.0;
            case SECONDS:
                return totalNanos / 1_000_000_000.0;
            case MINUTES:
                return totalNanos / 1_000_000_000.0 / 60.0;
            case HOURS:
                return totalNanos / 1_000_000_000.0 / 60.0 / 60.0;
            default:
                throw new UnsupportedOperationException("Time unit " + timeUnit + " is not supported");
        }
    }

    /**
     * Start only if not already running
     */
    public void start() {
        if (!_running)
        {
            _running = true;
            _startTime = System.nanoTime();
        }
    }

    /**
     * Pause or unpause, only if already running
     */
    public long pause() {
        if (_running) {
            if(!_paused) {
                _lastPausedTime = System.nanoTime();
            } else {
                _pausedTotalTime += System.nanoTime() - _lastPausedTime;
                _lastPausedTime = 0;
            }
            _paused = !_paused;
        }

        return _pausedTotalTime;
    }

    public double stop() {
        return stop(TimeUnit.MILLISECONDS);
    }
    /**
     * Stop, only if already running. Paused state does not matter.
     * @return the total running time, less time paused.
     */
    public double stop(TimeUnit timeUnit) {
        if (_running) {
            if (_paused) {
                // unpause and record total paused time.
                this.pause();
            }

            double totalTime = currentTime(timeUnit);
            // reset states, total time is reset upon next start
            _startTime = 0;
            _pausedTotalTime = 0;
            _running = false;
            return totalTime;
        }

        return 0;
    }
}
