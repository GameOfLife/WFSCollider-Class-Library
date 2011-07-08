+ WFSSynth {

    getUChain {
       var unit, sndFile;
       if([\disk,\buf].includes(this.audioType)) {
        sndFile = AbstractSndFile.fromType(this.audioType)
            .new(filePath, startFrame, startFrame+(dur*44100), pbRate, this.fadeTimes[0], this.fadeTimes[1]);
        unit = sndFile.makeUnit;
        unit.disposeOnFree = true;
        unit.setArg(\level,level)
       } {
        unit = U(\blipEnv,[\i_fadeInTime,this.fadeTimes[0], \i_duration, dur, \i_fadeOutTime, this.fadeTimes[1]])
       }
       ^UChain(unit,\output);
    }

}