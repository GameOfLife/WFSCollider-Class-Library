WFSPath2 {
	var <positions, >times, <c1, <c2;
	var <>name;
	var <>intType = \cubic; // \cubic, \bspline, \linear (future; add \quad, \step?)
	var <>curve = 1; // curve = 1: hermite
	var <>intClipMode = 'clip'; // 'clip', 'wrap', 'fold' // TODO for bspline	
	*new { |positions, times, name|
		^super.newCopyArgs( positions, times ).name_( name ).init;
	}
	
	init {
		times = times ?? {[1]};
		c1 = Order(); // user defined (relative, scaled to 0-1) control points are stored here
		c2 = Order();
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
	
/// CONTROLS ////////////////////////////////////////

	controls {
		var autoControls;
		if( (c1.size < positions.size) or: { c2.size < positions.size } )
			{ autoControls = this.generateAutoControls };
			
		^positions.collect({ |item, i|
				[ (c1[i] !? { c1[i].linlin( 0, 1, item, positions.wrapAt(i+1), \none ) }) 
					?? { autoControls[i][0] },
				  (c2[i] !? { c2[i].linlin( 0, 1, item, positions.wrapAt(i+1), \none ) }) 
				  	?? { autoControls[i][1] }
				];
			});
	}
	
	putC1 { |index, inC1, absolute = false| // index and inC1 can be arrays
		this.prPutC( index, inC1, absolute, c1 );
	}
	
	putC2 { |index, inC2, absolute = false|
		this.prPutC( index, inC2, absolute, c2 );
	}
	
	clearControls { [ c1, c2 ].do(_.makeEmpty) }
	trimControls { [ c1, c2 ].do({ |order|
				order = order.select({ |item, i| i < positions.size });
			});
	}
		
	generateAutoControls {
		if( positions.size > 1 )
		{ 
		^switch( intType.asString[0].toLower,
			$c, { positions.allSplineIntControls( curve / 3, intClipMode ).flop },
			$b, { (	positions.modeAt( (-4..-1), intClipMode ) ++ 
					positions ++ 
					positions.modeAt( positions.size + (..3), intClipMode ) )
						.bSplineIntControls.flop[4..positions.size+4]
				},
			$l, { positions.size.collect({ |i|
					var pts;
					pts = positions.modeAt( [i, i+1], intClipMode );
					[ 	pts[0].blend( pts[1], curve / 3 ),  
						pts[0].blend( pts[1], 1 - (curve / 3) ) 
					]
					}) ;  
				}
			); } 
		{ ^[  [ positions.first, positions.first ] ] };
	}
	
	prPutC { |index, inC, absolute = false, c|
		var array;
		c = c ? c1; 
		inC = inC.asCollection; 
		index = index.asCollection;
		
		if( absolute )
			{ array = index.collect({ |item, i| [ item, 
					inC.wrapAt( i ) !? { inC.wrapAt( i ).asPoint
						.linlin( positions.at( i ), positions.wrapAt( i + 1 ), 0, 1, \none ); }
					 ]; }); 
			}
			{ array = index.collect({ |item, i| [ item, 
					inC.wrapAt( i ) !? { inC.wrapAt( i ).asPoint } ]; }); 
			};
					
		array.do({ |item| c[ item[0] ] = item[1]; });
	}

	
//// COMPAT WITH OLD WFSPATH VERSION ////////////////////////////////////////

	forceTimes { |timesArray| times = timesArray.asCollection; }
	length { ^this.duration } 
	atTime2 { |time = 0, loop = true| ^this.atTime( time ); }
	
}
