/*
    Unit Library
    The Game Of Life Foundation. http://gameoflife.nl
    Copyright 2006-2011 Miguel Negrao, Wouter Snoei.

    GameOfLife Unit Library: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GameOfLife Unit Library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GameOfLife Unit Library.  If not, see <http://www.gnu.org/licenses/>.
*/

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