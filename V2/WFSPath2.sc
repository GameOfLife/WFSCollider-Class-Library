WFSPath2 {
	var <positions, >times;
	var <>name;
	var <>type = \cubic; // \cubic, \bspline, \linear (future; add \quad, \step?)
	var <>curve = 1; // curve = 1: hermite
	var <>clipMode = 'clip'; // 'clip', 'wrap', 'fold' // TODO for bspline	
	*new { |positions, times, name|
		^super.newCopyArgs( positions, times ).name_( name ).init;
	}
	
	init {
		times = times ?? {[1]};
	}
	
/// POSITIONS ////////////////////////////////////////

	positions_ { |pos| positions = pos.as( Array ).collect(_.asPoint); } // make sure it is valid
	
	// methods from old WFSPath
	x { ^positions.collect(_.x) }
	y { ^positions.collect(_.y) }
	
	distances { ^positions[1..].collect({ |pt, i| pt.dist( positions[i] );  }) } // between points
	
	// dimensions include controls:
	left { ^this.x.minItem.min( this.controls.flat.collect(_.x).minItem ) } 
	back { ^this.y.minItem.min( this.controls.flat.collect(_.y).minItem ) } // is top
	right { ^this.x.maxItem.max( this.controls.flat.collect(_.x).maxItem ) } 
	front { ^this.y.maxItem.max( this.controls.flat.collect(_.y).maxItem ) } // is bottom
	width { ^this.right - this.left }
	depth { ^this.front - this.back }
	
	asRect { ^Rect( this.left, this.back, this.width, this.depth ) 
		// note the reversed y axis; 
		}
		
		
/// TIMES ////////////////////////////////////////

	times { ^positions[1..].collect({ |item, i| times.wrapAt( i ) }); }
	
	duration { ^this.times.sum }	
	speeds { ^this.distances / this.times } // speeds in m/s
	
	asTimeIndexEnv { ^Env( [0] ++ this.times.collect({ |time, i| i+1 }), this.times ); }
	indexAtTime { |time = 0| ^this.asTimeIndexEnv.at( time ); }
	
	atTime { |time = 0|
		var index, controls;
		index = this.indexAtTime( time );
		controls = this.controls;
		^positions.wrapAt([ index.floor, index.floor+1])
			.splineIntFunction( index.frac, *(controls[index.floor]) ); 
		}	
		
/// POSITIONS AND TIMES /////////////////////////////

	at { |index|
		^[ positions.at( index ), times.clipAt( index ) ]
	}

	
/// CONTROLS ////////////////////////////////////////

	controls {
		^this.generateAutoControls
	}
		
	generateAutoControls { |inType, inClipMode, inCurve|
		if( positions.size > 1 )
		{ 
		^switch( (inType ? type).asString[0].toLower,
			$c, { positions.allSplineIntControls( (inCurve ? curve) / 3, 
					inClipMode ? clipMode ).flop },
			$b, { (	positions.modeAt( (-4..-1), inClipMode ? clipMode ) ++ 
					positions ++ 
					positions.modeAt( positions.size + (..3), inClipMode ? clipMode ) )
						.bSplineIntControls.flop[4..positions.size+4]
				},
			$l, { positions.size.collect({ |i|
					var pts;
					pts = positions.modeAt( [i, i+1], inClipMode ? clipMode );
					[ 	pts[0].blend( pts[1], curve / 3 ),  
						pts[0].blend( pts[1], 1 - (curve / 3) ) 
					]
					}) ;  
				}
			); } 
		{ ^[  [ positions.first, positions.first ] ] };
	}
	
/// SELECTION //////////////////////////////////////////

	
	copySelection { |indices, newName = "selection" | // indices should not be out of range!
		var pos, tim;
		indices = indices ?? { (..positions.size-1) };
		pos = positions[ indices ].collect(_.copy);
		tim = times[ indices[ 0..indices.size-2 ] ];
		^this.class.new( pos, tim, newName );
	}
	
	putSelection { |indices, selectionPath| // in place operation !!
		selectionPath = selectionPath.asWFSPath; 
		indices = indices ?? { (..selectionPath.positions.size-1) };
		indices.do({ |item, i|
			positions.put( item, selectionPath.positions[i].copy );
			if( i < selectionPath.times.size ) { times.put( item, selectionPath.times[i] ) };
		});	
	}	

//// COMPAT WITH OLD WFSPATH VERSION ////////////////////////////////////////

	forceTimes { |timesArray| times = timesArray.asCollection; }
	length { ^this.duration } 
	atTime2 { |time = 0, loop = true| ^this.atTime( time ); }
	
	asWFSPath {
		^WFSPath( positions.collect(_.asWFSPoint), times.clipAt( (0..positions.size-2) ) );
	}
	
	// these are still used in WFSPathEditor2
	intType { ^type }
	intClipMode { ^clipMode }
	intType_ { |type| this.type = type }
	intClipMode_ { |mode| this.clipMode = mode }
	
}
