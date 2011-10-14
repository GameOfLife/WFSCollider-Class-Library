/*
	Some WFS and Sync UGens for SC3 by Jan Trutzschler (c) sampleAndHold.org 2006
	
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/


ZeroCrossingCounter : UGen {
	*ar { arg in = 0.0;
		^this.multiNew('audio', in)
	}
	*kr { arg in = 0.0;
		^this.multiNew('control', in)
	}
 	checkInputs { ^this.checkSameRateAsFirstInput }
}


MultiUnPause : UGen {

	*ar { arg count = 0.0, range= 100;
		^this.multiNew('audio', count, range)
	}
// 	checkInputs { ^this.checkSameRateAsFirstInput }

}
