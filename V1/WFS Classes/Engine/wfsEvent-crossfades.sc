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

+ WFSSpeakerLine {
	
	switchDistances {    |wfsPoint = 0|  
		
		/// used by WFSConfiguration-switchDistances -> WFSPan / WFSPan2D
		/// this version of the calculation has no clicks at the crosspoint,
		/// but gets less accurate when moveng further away from the array
	
		var localAngle, deltaXSquared, deltaYSquared, distFromLine;
		wfsPoint = wfsPoint.asWFSPoint;
		
		if( WFSConfiguration.optLines )
			{ 
				if( WFSPath.azMax != 360 )
					{ localAngle = (this.first.angle / WFSPath.azMax) * 360; }
					{ localAngle = this.first.angle };
				
				case { localAngle == 0; }
					{ ^(this.first.y - wfsPoint.y) + 
							this.collect({ |sp| (sp.x - wfsPoint.x).abs }); }
					{ localAngle == 180; }
					{ ^(wfsPoint.y - this.first.y) + 
							this.collect({ |sp| (sp.x - wfsPoint.x).abs }); }					{ localAngle == 90; }
					{ ^(this.first.x - wfsPoint.x ) + 
							this.collect({ |sp| (sp.y - wfsPoint.y).abs }); }
					{ localAngle == 270; } 
					{ ^( wfsPoint.x - this.first.x ) + 
							this.collect({ |sp| (sp.y - wfsPoint.y).abs }); }
					{ true }
					{ ^this.distances( wfsPoint ) }
			};
		
		//^this.collect( _.distOpt( wfsPoint, deltaXSquared, deltaYSquared ) ) 
			// * this.distDirection( wfsPoint );
		
		}
	
	}

+ WFSConfiguration {
	switchDistances { |wfsPoint| ^speakerLines.collect( _.switchDistances( wfsPoint ) ).flat; }
	
	}

+ WFSPath {
	
	findSquareCrosspoints { |centerDist = 5.1, range = 0.1|
	
		// find the crosspoints for a square system.
		// array with indexes for points inside range or at start or end
		// of crossing segment
		
		// could use some optimisation
		
		var out;
		out = [];
		
		positions.histDo({ |item, last, i|
			if( last.notNil )
				{ 
				if( item.x.inRange( centerDist.neg, centerDist ) or: 
					last.x.inRange( centerDist.neg, centerDist ) )
					{ if( ( (item.y - centerDist).excess( range ).sign + 
							(last.y - centerDist).excess( range ).sign ).abs != 2 )
						{ if( out.includes( i -1).not ) { out = out.add( i -1) }; 
						  if( out.includes( i).not ) { out = out.add( i ) }; };
					  if( ( (item.y + centerDist).excess( range ).sign + 
					  	(last.y + centerDist).excess( range ).sign ).abs != 2 )
						{ if( out.includes( i -1).not ) { out = out.add( i -1) }; 
						  if( out.includes( i).not ) { out = out.add( i ) }; };
					};
				 
				if( item.y.inRange( centerDist.neg, centerDist ) or: 
					last.y.inRange( centerDist.neg, centerDist ) )
					{ if( ( (item.x - centerDist).excess( range ).sign + 
							(last.x - centerDist).excess( range ).sign ).abs != 2 )
						{ if( out.includes( i -1).not ) { out = out.add( i -1) }; 
						  if( out.includes( i).not ) { out = out.add( i ) }; };
					  if( ( (item.x + centerDist).excess( range ).sign + 
					  	(last.x + centerDist).excess( range ).sign ).abs != 2 )
						{ if( out.includes( i -1).not ) { out = out.add( i -1) }; 
						  if( out.includes( i).not ) { out = out.add( i ) }; };
					};
				  
				  };
		});
		^out;
		}
	
	}

+ WFSEvent {
	
	createCrossEvents { |intDiv = 10, centerDist = 5.1, range = 0.1|
		var intPath, crossPoints, newPaths, intPathTimeLine, timeOffsets;
		if( [ 'linear', 'cubic' ].includes( wfsSynth.intType ) )
			{  intPath = wfsSynth.wfsPath.copyNew.interpolate( intDiv, 'hermite', false );
			   crossPoints = intPath.findSquareCrosspoints( centerDist, range );
			   crossPoints = crossPoints.clumpSubsequent;
			   newPaths = [];
			   timeOffsets = [];
			   intPathTimeLine = intPath.timeLine;
			   crossPoints.do({ |item, i|
			   	newPaths = newPaths ++ [
			   		WFSPath( intPath.positions[ item ], 
			   			intPath.times[ item ].downSize(1),
			   			intPath.name.asString ++ "_cross" ++ i ) ];
			   	timeOffsets = timeOffsets ++ [ intPathTimeLine[ item[0] ] ];
			 	});
			   ^newPaths.collect({ |path, i|
			   	WFSEvent( 
			   		startTime + timeOffsets[i],
			   		wfsSynth.copyNew.wfsPath_( path ).startTime_( timeOffsets[i] ),
			   		track + 1
			   		);
			   	});
			   }
			{ ^[]; }
		}
	
	}