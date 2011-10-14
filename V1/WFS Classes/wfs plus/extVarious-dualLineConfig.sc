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

// WS 17/02/2009
// additions for dual line config support
// Also changed: extWFSConfiguration-cornerPoints.sc


+ WFSConfiguration {
	
	*rect4 { |xSpeakers = 40, ySpeakers = 56, dist = 0.165, xSize = 7.5, ySize = 11.0 |
	
		// if xSpeakers or ySpeakers == 0 no speakerLine is created
		// dual line : .rect4( 0, 12*8, 0.165, 5, 0 ); -- 5 = width
		
		^WFSConfiguration( 
			*( { |i|
				if([xSpeakers, ySpeakers].wrapAt(i) > 0)
					{	WFSSpeakerLine.newFromCenter( 
							[	[ 0, ySize / 2, 0, 180],
								[ xSize / 2, 0, 0, 270],
								[0, ySize.neg / 2, 0, 0 ],
								[xSize.neg / 2, 0, 0, 90] ][i],
							[xSpeakers, ySpeakers].wrapAt(i),
							dist)
					};
			 } ! 4 ).select(_.notNil)
				 
			).name_( ("rect4_" ++ xSpeakers ++ "x_" ++ ySpeakers ++ "y").asSymbol  );

		}
		
	partial { |index = 0, max = 2|
	
		// split a conf up into multiple parts, per speakerLine 
		// might not always be the best option, but works in regular cases
		
		// Q: DO I NEED TO COPY MORE? CHECK STARTUP FILES !!
		
		var nSplPerIndex, startIndex, endIndex;
		nSplPerIndex = speakerLines.size/max;  
		startIndex = (nSplPerIndex * index).asInt;  
		endIndex = ((nSplPerIndex * (index+1)) - 1).asInt; 
		^WFSConfiguration( *( speakerLines[startIndex..endIndex] ) )
			.cornerPoints_( this.cornerPoints[startIndex..endIndex] )
			.name_( name ++ "_i" ++ (index+1) );	
		}
	
	}