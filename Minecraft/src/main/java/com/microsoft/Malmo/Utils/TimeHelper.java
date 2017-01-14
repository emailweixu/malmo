// --------------------------------------------------------------------------------------------------
//  Copyright (c) 2016 Microsoft Corporation
//  
//  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
//  associated documentation files (the "Software"), to deal in the Software without restriction,
//  including without limitation the rights to use, copy, modify, merge, publish, distribute,
//  sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
//  
//  The above copyright notice and this permission notice shall be included in all copies or
//  substantial portions of the Software.
//  
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
//  NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
//  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
//  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// --------------------------------------------------------------------------------------------------

package com.microsoft.Malmo.Utils;

import java.lang.reflect.Field;

import com.microsoft.Malmo.MalmoMod;

import net.minecraft.client.Minecraft;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.Timer;
import net.minecraft.world.World;

/** Time-based methods and helpers.<br>
 * Time is usually measured in some form of game tick (eg WorldTick etc). In the normal course of operations,
 * these take places 20 times a second - hence a MillisencondsPerWorldTick value of 50.
 * If the game is overclocked, this is no longer true, but generally it still makes sense to deal with a simple multiple of game ticks,
 * so we leave MillisecondsPerWorldTick unchanged.
 */
public class TimeHelper
{
    public final static float MillisecondsPerWorldTick = 50.0f;
    public final static float MillisecondsPerSecond = 1000.0f;
    public static long serverTickLength = 50;
    public static boolean lockStepped = false;
    public static long displayGranularityMs = 0;  // How quickly we allow the Minecraft window to update.
    private static long lastUpdateTimeMs;

    /**
     * Set ms per tick for the mission
     * @param msPerTick milliseconds per tick
     */
    public static void setMsPerTick(long msPerTick) {
        serverTickLength = msPerTick;
        // lockstep is only supported in integrated server mode.
        lockStepped = msPerTick <= 1 && MalmoMod.instance.isIntegratedServer;
    }
    /** Very simple stopwatch-style timer class; times in WorldTicks.
     */
    static public class WorldTimer
    {
        private World world;
        private long startTime = 0;
        private long stopTime = 0;
        
        public WorldTimer(World world)
        {
            this.world = world;
        }
        
        /** Start timing
         */
        public void start()
        {
            this.startTime = this.world.getTotalWorldTime();
            this.stopTime = 0;
        }
        
        /** Stop timing
         */
        public void stop()
        {
            this.stopTime = this.world.getTotalWorldTime();
        }
        
        /** Get the timed duration, converted into what would be milliseconds if no over-clocking has occurred.<br>
         * If stop() has been called, returns the time between calls to stop() and start().
         * If start() has been called but not stop, returns the time since start() was called.<br>
         * It is up to the user to avoid doing things in a stupid order.
         * @return the measured duration
         */
        public float getDurationInMs()
        {
            long duration = (stopTime != 0) ? this.stopTime - this.startTime : this.world.getTotalWorldTime() - this.startTime;
            return duration * MillisecondsPerWorldTick;
        }
    }
    /**
     * Set ms per tick from client side.
     * @param msPerTick milliseconds per tick
     * @return true if success
     */
    static public boolean setMsPerTickForClient(long msPerTick)
    {
        boolean devEnv = (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
        // We need to know, because the member name will either be obfuscated or not.
        String timerMemberName = devEnv ? "timer" : "field_71428_T";
        // NOTE: obfuscated name may need updating if Forge changes - search for "timer" in Malmo\Minecraft\build\tasklogs\retromapSources.log
        Field timer;
        try
        {
            timer = Minecraft.class.getDeclaredField(timerMemberName);
            timer.setAccessible(true);
            if (msPerTick > 1)
                timer.set(Minecraft.getMinecraft(), new Timer(1000 / msPerTick));
            else
                timer.set(Minecraft.getMinecraft(), new LockSteppedTimer());
            setMsPerTick(msPerTick);
            return true;
        }
        catch (SecurityException e)
        {
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
        catch (IllegalArgumentException e)
        {
            e.printStackTrace();
        }
        catch (NoSuchFieldException e)
        {
            e.printStackTrace();
        }
        return false;
    }
    
    static public void updateDisplay()
    {
        long timeNow = System.currentTimeMillis();
        if (timeNow - lastUpdateTimeMs > displayGranularityMs)
        {
            Minecraft.getMinecraft().updateDisplay();
            lastUpdateTimeMs = timeNow;
        }
    }
}
