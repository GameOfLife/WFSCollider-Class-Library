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

// A configuration class for the wfs system containing information about speaker positions


WFSConfiguration {

	classvar <default;
	classvar <>optLines = true;

	var <>speakerLines, <>speakerArray, <>name, <allAnglesFrom0, <allRhos;
	var <>useSwitch = true, <>useCrossFades = true, <>fadeRange = 0.1;
	var >cornerPoints; // new 22/11/08
	
	*initClass { default = WFSConfiguration.rect2.initForPlane; } // replace with correct one later
	
	*default_ { |newDefault| 
		default = newDefault ? default;
		WFSPan2D.defaultSpeakerSpec = default; // change the WFSPan2D default too
		}
		
	*emptyNew { ^super.newCopyArgs( [], WFSSpeakerArray[], 'new' ) }
	
	*new { arg ... args;
		var lines = [], array = [], speakers = [];
		args.do { |item|
			case { item.class == WFSSpeakerLine }
				{ lines = lines.add( item ) }
				{ item.class == WFSSpeakerArray }
				{ array = array ++ item  }
				{ item.class == WFSSpeaker }
				{ speakers = speakers.add( item ) };
			} ;
		if( speakers.size > 0)
			{ array = array ++ speakers };
		array = array.asWFSSpeakerArray;
		
		^super.newCopyArgs( lines, array, 'new' )
		}
		
	*rect { |xSpeakers = 40, ySpeakers = 56, dist = 0.164|
		var xSize = (xSpeakers * dist) / 2;
		var ySize = (ySpeakers * dist) / 2;
		^WFSConfiguration( 
			*( { |i|
				WFSSpeakerLine.newFromCenter( 
					[	[ 0, ySize, 0, 180],
						[ xSize, 0, 0, 270],
						[0, ySize.neg, 0, 0 ],
						[xSize.neg, 0, 0, 90] ][i],
					[xSpeakers, ySpeakers].wrapAt(i),
					dist)
				 } ! 4 )
			).name_( ("rect_" ++ xSpeakers ++ "x_" ++ ySpeakers ++ "y").asSymbol  );
		}
		
	*rect2 { |xSpeakers = 48, ySpeakers = 48, dist = 0.165, xSize = 10.0, ySize = 10.0 |
		^WFSConfiguration( 
			*( { |i|
				WFSSpeakerLine.newFromCenter( 
					[	[ 0, ySize / 2, 0, 180],
						[ xSize / 2, 0, 0, 270],
						[0, ySize.neg / 2, 0, 0 ],
						[xSize.neg / 2, 0, 0, 90] ][i],
					[xSpeakers, ySpeakers].wrapAt(i),
					dist)
				 } ! 4 )
			).name_( ("rect2_" ++ xSpeakers ++ "x_" ++ ySpeakers ++ "y").asSymbol  );
		}
		
	*halfRect2_1 { |xSpeakers = 48, ySpeakers = 48, dist = 0.165, xSize = 10.0, ySize = 10.0 |
		var fullRect;
		fullRect = this.rect2( xSpeakers, ySpeakers, dist, xSize, ySize );
		^WFSConfiguration( 
			*fullRect.speakerLines[[0,1]]
			).name_( ("hr2_1" ++ xSpeakers ++ "x_" ++ ySpeakers ++ "y").asSymbol )
			.cornerPoints_( fullRect.cornerPoints[[0,1]] ); 
		}
	
	*halfRect2_2 { |xSpeakers = 48, ySpeakers = 48, dist = 0.165, xSize = 10.0, ySize = 10.0 |
		var fullRect;
		fullRect = this.rect2( xSpeakers, ySpeakers, dist, xSize, ySize );
		^WFSConfiguration( 
			*fullRect.speakerLines[[2,3]]
			).name_( ("hr2_2" ++ xSpeakers ++ "x_" ++ ySpeakers ++ "y").asSymbol )
			.cornerPoints_( fullRect.cornerPoints[[2,3]] ); 
		}
		
	/*
	*halfRect2_1 { |xSpeakers = 48, ySpeakers = 48, dist = 0.165, xSize = 10.0, ySize = 10.0 |
		^WFSConfiguration( 
			*( { |i|
				WFSSpeakerLine.newFromCenter( 
					[	[ 0, ySize / 2, 0, 180],
						[ xSize / 2, 0, 0, 270],
					][i],
					[xSpeakers, ySpeakers].wrapAt(i),
					dist)
				 } ! 2 )
			).name_( ("hr2_1" ++ xSpeakers ++ "x_" ++ ySpeakers ++ "y").asSymbol  );
		}
		
	*halfRect2_2 { |xSpeakers = 48, ySpeakers = 48, dist = 0.165, xSize = 10.0, ySize = 10.0 |
		^WFSConfiguration( 
			*( { |i|
				WFSSpeakerLine.newFromCenter( 
					[	[0, ySize.neg / 2, 0, 0 ],
						[xSize.neg / 2, 0, 0, 90] 
					][i],
					[xSpeakers, ySpeakers].wrapAt(i),
					dist)
				 } ! 2 )
			).name_( ("hr2_2" ++ xSpeakers ++ "x_" ++ ySpeakers ++ "y").asSymbol  );
		}
	*/
		
	*rect3 {  ^this.rect2( 40, 56, 0.165, 7, 12 ); }	
	*halfRect3_1 {  ^this.halfRect2_1( 40, 56, 0.165, 7, 12 ); }
	*halfRect3_2 {  ^this.halfRect2_2( 40, 56, 0.165, 7, 12 ); }
	
	
	*stereo { |width = 0.19, relativeAngle = 0| // headphones (or any other 2 speakers)
			// relAngle ==   0: faced towards each other
			//    "     == -90: faced to the back
			//    "     ==  90: faced to the front
			// etc..
		^WFSConfiguration( WFSSpeakerArray[ 
			WFSSpeaker( width.neg / 2, 0, 0, (90 - relativeAngle) % 360 ),
			WFSSpeaker( width / 2, 0, 0, ( 270 + relativeAngle ) % 360 )] ).name_(
				 ( "stereo_" ++ width ++ "m" ).asSymbol )
				 .useCrossFades_( false );
		}
		
	*headphones { ^this.stereo( 0.19, -90 ).name_( "headphones" ); } 
	*powerbookSpeakers { ^this.stereo( 0.31, -90 ).name_( "powerbookSpeakers" ); }
	*stereoSpeakers { |width = 1.3| ^this.stereo( width, -90 ) }
	
	*singleArray { |position = 0, size = 8, distBetween = 0.165, angle = 180|
		^this.new( WFSSpeakerLine.newFromCenter( 
				position.asWFSSpeaker.angle_( angle ), size, distBetween ) )
			.name_( "array_%".format( size ) ).useCrossFades_( false );
		}
		
	*stereoLine {  |width = 0.19, position | // headphones (or any other 2 speakers)
			// relAngle ==   0: faced towards each other
			//    "     == -90: faced to the back
			//    "     ==  90: faced to the front
			// etc..
		position = position ? [0,5];
		^this.singleArray( position, 2, width, 180)
			.name_( ( "stereoLine_" ++ width ++ "m" ).asSymbol );
		}
	
	*mono { |position = 0|
		^this.new( position.asWFSSpeaker ).name_( "mono" ).useCrossFades_( false );
		}
		
	*quad { |width = 5, depth = 8| // quad speaker system - clockwise
		width = width / 2;
		depth = depth / 2
		^WFSConfiguration( WFSSpeakerArray[ 
			[ width.neg, depth,     0, 135 ],
			[ width,     depth,     0, 225],
			[ width,     depth.neg, 0, 315],
			[ width.neg, depth.neg, 0, 45]] 
			).name_( ( "quad_" ++ width ++ "x" ++ depth ++ "m" ).asSymbol )
				.useCrossFades_( false );
		}
		
	*line {  |angleFromCenter = 0, 
			distFromCenter = 2, nSpeakers = 8, distBetween = 0.164, xyOffset = 0|
		^WFSConfiguration( 
			WFSSpeakerLine.newFromCenter( 
				WFSSpeaker.newAZ( angleFromCenter, distFromCenter ) + xyOffset,
				nSpeakers, distBetween ) ).name_( "line_%".format( nSpeakers ) )
				.useCrossFades_( false );
		}
		
	// 2D rect support
	
	asRect { ^this.allSpeakers.asRect }
	
	width { ^this.asRect.width }
	depth { ^this.asRect.height }
	
	maxArea { // size in square meters for rectangle configuration m^2
		^this.width * this.depth;
		}
		
	center { ^this.allSpeakers.center; // should be around zero
		}
	
	rectCenter { ^this.allSpeakers.rectCenter }
		
	center_ { |position = 0|
		var center;
		center = this.center;
		position = position.asWFSPoint;
		
		speakerLines = speakerLines.collect( { |line|
			line.move( position - center ); } );
		speakerArray = speakerArray.move( position - center );		}
		
	rectCenter_ { |position = 0|
		var center;
		center = this.rectCenter;
		position = position.asWFSPoint;
		
		speakerLines = speakerLines.collect( { |line|
			line.move( position - center ); } );
			
		speakerArray = speakerArray.move( position - center );		}
		
	move { |moveBy = 0|	
		^WFSConfiguration.emptyNew
			.speakerLines_(
				speakerLines.collect( { |line| line.move( moveBy ); } )
				)
			.speakerArray_( speakerArray.move( moveBy ) )
			.name_( name );
		}
	
	++ { |aWFSConfiguration|
		if( aWFSConfiguration.class != WFSConfiguration )
			{ aWFSConfiguration = WFSConfiguration( *aWFSConfiguration  ); };
		  ^WFSConfiguration( *(
		  	speakerLines ++ aWFSConfiguration.speakerLines ++
		  	speakerArray ++ aWFSConfiguration.speakerArray ) );
		 }
	
	radianAnglesFrom { |point = 0|
		^this.allSpeakers.collect({ |speaker|
			speaker.radianAngleFrom( point );
			});
		}
		
	closestSpeakerIndex { |point = 0, clip= true|
	
		//find the index of the speaker nearest to given point
		
		var speakerLineDistances, lineClosestIndexes, count = 0;
		var linesIndex, arrayIndex, arrayDist = inf, linesDist = inf;
		
		point = point.asWFSPoint;
		
		if( speakerLines.size > 0 )
			{ speakerLineDistances = speakerLines.collect({ |line|
				line.shortestDistFrom( point ); });
			
			lineClosestIndexes = speakerLines.collect({ |line, i|
				var out;
				out = line.closestSpeakerIndex( point, clip ) + count;
				count = count + line.size;
				out;
				});
			
			linesIndex = MinItem.switch( speakerLineDistances, lineClosestIndexes );
			};
			
		if( speakerArray.size > 0 )
			{  if( linesIndex.notNil )
					{ linesDist = this.allSpeakers[ linesIndex.round(1) ].dist( point ); };
				arrayIndex = speakerArray.closestSpeakerIndex( point ) + count;
				arrayDist = this.allSpeakers[ arrayIndex ].dist( point ); 
				^MinItem.switch( [ arrayDist, linesDist ], [ arrayIndex, linesIndex ] );
			}
			{ ^linesIndex };
		
		^0;
			
		}
	
		
	anglesFrom { |point = 0|	
		^this.radianAnglesFrom( point )
			.collect({ |angle| WFSTools.fromRadians( angle ) });
		}
	
	distancesFrom { |point = 0|
		^this.allSpeakers.collect({ |speaker|
			speaker.dist( point );
			});
		}
		
	postInfo {
		"WFSSpeakerLines: %\nWFSSpeakerArray: %\n"
			.postf( speakerLines.size, speakerArray.size);
		}
	
	printOn {arg stream;
		stream << "WFSConfiguration( " << name << " )";
		}
	
	allSpeakers {
		var out = [];
		speakerLines.do { |item|
			item.do { |subItem|
				out = out.add( subItem );
				}
			};
		^out.asWFSSpeakerArray ++ speakerArray;
		}
		
	size { ^speakerLines.collect( _.size ).sum + speakerArray.size; }
		
	plot { this.allSpeakers.plot; }
	
	plotSmoothInput {  |size, color, fromRect, allSpeakers = false|
		if( allSpeakers )
			{ ^this.allSpeakers.plotSmoothInput( size, color, fromRect ); }
			{ speakerLines.asCollection
				.do({ |speakerLine| 
					speakerLine.plotSmoothInput( size, color, fromRect ) });
			  speakerArray.asWFSSpeakerArray.plotSmoothInput( size, color, fromRect );
			};
		}
			
	plotSmooth { |allSpeakers = false, toFront = true|
		var window;
		var fromRect;
		
		/*
		window = SCWindow( name , Rect(128, 64, 400, 400)).front;
		window.view.background_(Color.black );
		*/
		
		window = WFSPlotSmooth( name, toFront: toFront );
		fromRect = this.asRect;
		
		window.drawHook = { var tempPath, tempPath2, firstPoint, bounds;
			bounds = [window.view.bounds.width, window.view.bounds.height]; 
			bounds = bounds.minItem;
			this.plotSmoothInput( bounds, fromRect: fromRect, allSpeakers: allSpeakers  ); 
			};
			
		window.refresh;
	
		}
	
	squareCenterDist { // assume a square setup with equal distances from center
		var cd; cd = 0;
		speakerLines[0] !? { cd = speakerLines[0].shortestDistFromCenter.abs; }
		^cd;
		}
		
	rectCenterDist { // assume a rectangular setup -> cd as [x,y]
		var cd; cd = [0,0];
		speakerLines[0] !? { 
			cd[0] = speakerLines.select({ |sl| sl.orientation == \v; })[0];
			cd[1] = speakerLines.select({ |sl| sl.orientation == \h; })[0];
			};
			
		cd = cd.collect({ |item| if( item.notNil ) { item.shortestDistFromCenter.abs } { inf } });
		^cd;
		}
		
	distanceToSquareSides { |point = 0| // negative when inside, positive when outside
		// for crossfade at crosspoint
		
		//// CHANGED FOR RECTANGULAR !!!
		
		var cd;
		cd = this.rectCenterDist;
		cd = [ cd, cd.neg ];
		point = point.asWFSPoint;
		^MaxItem( [ 
			point.x - cd[0][0],
			cd[1][0] - point.x,			
			point.y - cd[0][1],
			cd[1][1] - point.y
			], true );
		}
		
	/*
	distanceToSquareSides { |point = 0| // negative when inside, positive when outside
		// for crossfade at crosspoint
		
		//// CHANGE FOR RECTANGULAR !!!
		
		var cd;
		cd = this.squareCenterDist;
		cd = [ cd, cd.neg ];
		point = point.asWFSPoint;
		^MaxItem( [ 
			point.x - cd[0],
			cd[1] - point.x,			
			point.y - cd[0],
			cd[1] - point.y
			], true );
		}
	*/
	
	distances { |point = 0| ///  used by WFSPan / WFSPan2D
		if( useSwitch )
			{ ^(speakerLines.collect( _.distances( point ) ) 
				++ speakerArray.distances(point) ).flat; }
			{ ^(speakerLines.collect({ |line| line.distances( point ).abs }) 
				++ speakerArray.distances(point).abs ).flat;  }
		}
	
	radianAnglesFrom0 { 
		if( allAnglesFrom0.isNil )
			{ allAnglesFrom0 = this.radianAnglesFrom( WFSPoint(0,0,0) ); };
		^allAnglesFrom0;
		}
	
	distancesFrom0 {  // distanceFrom0 a.k.a. rho
		if( allRhos.isNil )
			{ allRhos = this.distancesFrom( WFSPoint(0,0,0) ); };
		^allRhos;
		}
		
	initForPlane { this.radianAnglesFrom0; this.distancesFrom0; }
		
	distancesToPlane { |plane|
		if( plane.class != WFSPlane ) 
			 { "input for WFSConfiguration-distancesToPlane should be a WFSPlane".postln };
		this.initForPlane; // calculate speaker distances only once
		^allAnglesFrom0.collect({ |angleFrom0, i|
			plane.basicDist( angleFrom0, allRhos[i] ); 
			});
		}
	
	asSVGObject { |scale, move|
		^SVGGroup( speakerLines.collect({ |line|
				line.asSVGObject( scale, move ); }),
			"SpeakerConfiguration" );	
		}
		
	
}