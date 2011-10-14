/*
    GameOfLife WFSCollider - Wave Field Synthesis spatialization for SuperCollider.
    The Game Of Life Foundation. http://gameoflife.nl
    Copyright 2006-2010 Wouter Snoei.

    GameOfLife WFSCollider software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GameOfLife WFSCollider is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GameOfLife WFSCollider.  If not, see <http://www.gnu.org/licenses/>.
*/

+ SimpleNumber {
	plotSmoothInput { |size, color, fromRect|
		WFSConfiguration.default.allSpeakers.wrapAt( this )
			.asWFSPoint.plotSmoothInput( size, color, fromRect )
		}
		
	plotSmooth {  |speakerConf = \default|
		if( speakerConf == \default )
			{ speakerConf = WFSConfiguration.default };
		speakerConf.allSpeakers.wrapAt( this )
			.asWFSPoint.plotSmooth( speakerConf );
		}
	}