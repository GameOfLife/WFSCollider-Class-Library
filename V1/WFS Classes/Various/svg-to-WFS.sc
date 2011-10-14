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

+ Nil { edit { |parent| ^nil } *edit { |parent| ^nil } }

+ SVGObject {

	asWFSPath { |scale = 20, move = 300, length = 1| ^nil }
	
	}
	
+ SVGLine {

	asWFSPath { |scale = 20, move = 300, length = 1| 
		^WFSPath( [ x1@y1, x2@y2 ].collect({ |point|
				( (point - move) / scale ).asWFSPoint.flipY }),
			name: (name ?? { this.class.asString ++ "_*" }).asSymbol ).length_( length );
		 }

	}
	
+ SVGPolyLine {

	asWFSPath { |scale = 20, move = 300, length = 1|  // scale & move is backwards
		^WFSPath( points.collect({ |point| ( (point - move) / scale ).asWFSPoint.flipY }), 
			name: (name ?? { this.class.asString ++ "_*" }).asSymbol ).length_( length ) 
		}
	
	}
	
+ SVGPolygon {

	asWFSPath { |scale = 20, move = 300, length = 1| 
		^WFSPath(
			(points ++ [points.first])
				.collect({ |point| ( (point - move) / scale ).asWFSPoint.flipY }), 
			name: (name ?? { this.class.asString ++ "_*" }).asSymbol ).length_( length ) 
		}
	}
	
+ SVGPath {
	asWFSPath { |scale = 20, move = 300, length = 1, useCurves = false|  // no curves yet
		if( useCurves )
			{ ^WFSPath( 
				this.asPolySpline.asPolyLine( WFSPath.svgImportCurveResolution )
					.points
					.collect({ |point| ( (point - move) / scale ).asWFSPoint.flipY }), 
				name: (name ?? { this.class.asString ++ "_*" }).asSymbol ).length_( length )  }
			{ ^WFSPath( this.asPoints
				.collect({ |point| ( (point - move) / scale ).asWFSPoint.flipY }),
					name: (name ?? { this.class.asString ++ "_*" }).asSymbol
					).length_( length ) };
		}
	}

+ SVGRect {
	asWFSPath { |scale = 20, move = 300, length = 1| 
		^WFSPath.rect(
			( x - move.asPoint.x ) / scale, 
			(( y - move.asPoint.y ) / scale).neg, 
			width / scale, height / scale, 
			[rx, ry] / scale, 
			name: (name ?? { this.class.asString ++ "_*" }).asSymbol ).length_( length ) 
		}
	}
	
+ WFSPathArray {

	asSVGFile { arg path = "~/scwork/wfsPathsOut.svg", scale = 20, move = 300, simpleMode = true;	
		^if( simpleMode == true )
			{
			SVGFile( path, 
				[ WFSConfiguration.default.asSVGObject( scale, move ) ] ++
				this.collect({ |wfsPath|
					wfsPath.asSVGPath( scale, move, curve: true ) }) );  
			} {
			SVGFile( path, 
				[ WFSConfiguration.default.asSVGObject( scale, move ) ] ++
				this.collect({ |wfsPath|
					wfsPath.asSVGGroup( scale, move, false ) })					 ); };
		}
		
	*fromSVGFile { |svgFile, scale = 20, move = 300, length = 1, useCurves = false |
		var objects, paths;
		objects = svgFile.objects.select({ |item| item.name.asSymbol != "SpeakerConfiguration" });
		paths = objects.collect({ |item| item.asWFSPath( scale, move, length, useCurves ) })
			.select( _.notNil );
		^this.with( *paths );
		
		}
	
	writeSVGFile {
		arg path = "~/scwork/wfsPathsOut.svg", scale = 20, move =300,
			overwrite= false, ask= true, simpleMode = \ask;
			
		var file, doItFunc;
		
		doItFunc = {
			file = this.asSVGFile( path, scale, move, simpleMode );
			if( path.notNil )
				{ file.write( overwrite, ask, true ); }
				{ file.format.postln; };
			};
			
		if( simpleMode == \ask )
			{  SCAlert( "Write SVGFile :: Please select a mode",
 					[ "cancel", "simple", "styled"],
 					[ nil, { simpleMode = true; doItFunc.value },
 						{ simpleMode = false; doItFunc.value } ] );
 				}
 			{ doItFunc.value };
		}
	
	/*
		
	writeSVGFile {arg path = "~/scwork/wfsPathsOut.svg", scale = 20, move =300,
			overwrite= false, ask= true, simpleMode = \ask;
			
		var file, simpleFunc, styledFunc, doItFunc;
		
		styledFunc = {
			file = SVGFile( path, this.collect({ |wfsPath|
				wfsPath.asSVGGroup( scale, move, false ) }) ++ 
					[ 
					WFSPath.centerAsSVGGroup( scale, move ), 
					WFSPath.scaleMessage( scale ) ] ); };
					
		simpleFunc = {
			file = SVGFile( path, this.collect({ |wfsPath|
				wfsPath.asSVGPolyLine( scale, move ) }) ++ 
					[ 
					WFSPath.centerAsSVGGroup( scale, move ), 
					WFSPath.scaleMessage( scale ) ] );  };
		
		doItFunc = {
			
			if( simpleMode == true )
				{ simpleFunc.value; }
				{ styledFunc.value; };
			
			if( path.notNil )
				{ file.write( overwrite, ask, true ); }
				{ file.format.postln; };
				
			};
			
		if( simpleMode == \ask )
			{  SCAlert( "Write SVGFile :: Please select a mode",
 					[ "cancel", "simple", "styled"],
 					[ nil, { simpleMode = true; doItFunc.value },
 						{ simpleMode = false; doItFunc.value } ] );
 				}
 			{ doItFunc.value };
	}
	
	*/
	
	readSVGFile { arg path = "~/scwork/wfsPathsOut.svg"; }
}
	