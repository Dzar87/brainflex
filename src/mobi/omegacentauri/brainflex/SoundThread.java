/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobi.omegacentauri.brainflex;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import javax.sound.sampled.*;
import java.util.Properties;

/**
 *
 * @author Klaus Horn - klaus.horn@metropolia.fi
 */
public class SoundThread extends Thread {
    
    private int meditation = 0;
    private int attention = 0;
    private boolean done = false;
    private AudioInputStream audioInputStream = null;
    private Clip clip = null;
    private FloatControl gainControl = null;
    private float min = -90.0f, max = 5.0f; // will set the values to max and min by default
    private float meditationThreshold = 50.0f, step = 2.50f; // defaults
    private float attentionThreshold = 50.0f;
    private int threadDelay = 1000;
    private File soundFile = null;
    
    SoundThread(String threadName) {
        this.setName(threadName);
        this.loadProperties();
        this.loadWav();
    }
            
    @Override
    public void run() {
        // Should really do a check if the file exists or not. Very dirty...
        if (this.clip == null) {
            return;
        }
        this.gainControl = (FloatControl) this.clip.getControl(FloatControl.Type.MASTER_GAIN);
        this.clip.loop(Clip.LOOP_CONTINUOUSLY);
        float max_limit = this.gainControl.getMaximum();
        float min_limit = this.gainControl.getMinimum();
        //System.out.println("MAX " + max_limit);
        //System.out.println("MIN " + min_limit);
        // check limits are within bounds
        if (this.min < min_limit) {
            this.min = min_limit;
        }
        if (this.max > max_limit) {
            this.max = max_limit;
        }
        if (this.step <= 0.0) {
            this.step = 2.50f;
        }
        if (this.step >= 10.0) {
            this.step = 10.0f;
        }
        // Start volume from min
        float volume = this.min;
        this.gainControl.setValue(volume);
        this.clip.start();
        System.out.println(this.clip.isRunning());
        while (!this.done) {
            synchronized (this) {
                if (this.meditation > this.attention && this.meditation > this.meditationThreshold) {
                    System.out.println("MEDITATING**MEDITATING**MEDITATING**MEDITATING**");
                    volume += this.step;
                    if (volume >= this.max) {
                        this.gainControl.setValue(this.max);
                        volume = this.max;
                    } else
                        this.gainControl.setValue(volume);
                } else if (this.attention > this.meditation && this.attention > this.attentionThreshold) {
                    System.out.println("ATTENTION**ATTENTION**ATTENTION**ATTENTION**");
                    volume -= this.step;
                    if (volume <= this.min) {
                        this.gainControl.setValue(this.min);
                        volume = this.min;
                    } else
                        this.gainControl.setValue(volume);
                }
            }
            System.out.println("VOLUME: " + this.gainControl.getValue());
            try {
                Thread.sleep(this.threadDelay);
            } catch (InterruptedException e) {
                this.getStackTrace();
            } catch (Exception e) {
                this.getStackTrace();
            }
        }
    }
    
    public void end() {
        this.done = true;
        if (this.clip != null) {
            this.clip.stop();
            this.clip.close();
        }    
        System.out.println(this.getName() + " terminated.");
    }
    
    public synchronized void setMedAttValue(int value, AttentionType type) {
        if (type == AttentionType.ATTENTION) 
            this.attention = value;
        else if (type == AttentionType.MEDITATION)
            this.meditation = value;
    }
    
    private void loadProperties() {
        Properties soundProperties = new Properties();
        FileInputStream file;
        // base folder is ./
        String path = "./resources/sound.properties";
        try {
            // load the file handle for sound.properties
            file = new FileInputStream(path);

            // load all the properties from this file
            soundProperties.load(file);

            // close stream once loaded
            file.close();
            this.min = Float.valueOf(soundProperties.getProperty("min"));
            this.max = Float.valueOf(soundProperties.getProperty("max"));
            this.step = Float.valueOf(soundProperties.getProperty("step"));
            this.meditationThreshold = Float.valueOf(soundProperties.getProperty("meditationThreshold"));
            this.attentionThreshold = Float.valueOf(soundProperties.getProperty("attentionThreshold"));
            this.threadDelay = Integer.valueOf(soundProperties.getProperty("delay"));
            
            // generic pokemon exception handling cuz i can't be bothered...
        } catch (Exception e){
            e.printStackTrace();
        }
        
        // Debug print
        System.out.println("Volume min: " + this.min);
        System.out.println("Volume max: " + this.max);
        System.out.println("Volume step: " + this.step);
        System.out.println("Meditation Threshold: " + this.meditationThreshold);
        System.out.println("Attention Threshold: " + this.attentionThreshold);
        System.out.println("soundThread delay: " + this.threadDelay);
    }
    
    private void loadWav() {
        // Dynamic .wav file loading from resource folder relative to cwd.
        try {
            Path cwd = Paths.get("./resources/");
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(cwd, "*.wav")) {
                // Return first .wav file path if there is one.
                Iterator itr = stream.iterator();
                if (itr.hasNext()) {
                    this.soundFile = new File(itr.next().toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (this.soundFile != null) {
                System.out.println(soundFile.toString());
                this.audioInputStream = AudioSystem.getAudioInputStream(this.soundFile);
                this.clip = AudioSystem.getClip();
                this.clip.open(this.audioInputStream);
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }
}
