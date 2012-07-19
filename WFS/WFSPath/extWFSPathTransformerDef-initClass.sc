/*
    GameOfLife WFSCollider - Wave Field Synthesis spatialization for SuperCollider.
    The Game Of Life Foundation. http://gameoflife.nl
    Copyright 2006-2011 Wouter Snoei.

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

+ WFSPathTransformerDef {
	
	// define a bunch of default WFSPathTransformerDefs
	
	*initClass {
		
		WFSPathTransformerDef( \name,
			 { |f, obj| obj.name_( f.get( \name ) ) }, 
		 	[ \name, "" ], 
		 	{ |f, obj| [ \name, obj.name ] } 
		 )	
		 	.setSpec( \name, StringSpec() )
			.useSelection_( false );
				
		WFSPathTransformerDef( \type, 
			 { |f, obj| obj
			 	.type_( f.get( \type ) )
			 	.curve_( f.get( \curve ) )
			 	.clipMode_( f.get( \clipMode ) );
			}, 
		 	[ \type, \bspline, \curve, 1.0, \clipMode, \clip ], 
		 	{ |f, obj| [ \type, obj.type, \curve, obj.curve, \clipMode, obj.clipMode ] } 
		 )
		 	.setSpec( \type, ListSpec( [ \bspline, \cubic, \linear ] ) )
		 	.setSpec( \curve, ControlSpec( 0, 2, \lin, 0.1, 1 ) )
		 	.setSpec( \clipMode, ListSpec( [ \clip, \wrap, \fold ] ) )
		 	.useSelection_( false );
		 
			 	
		WFSPathTransformerDef( \move, 
			{ |f, path| 
				var toCenterAmt = 0;
				if( f.get( \center ) == true ) {
					toCenterAmt = path.positions.mean
				};
				path.positions_( path.positions + [f.get( \move ) - toCenterAmt ] ); 
			}, 
			[ \center, false, \move, 0@0 ] 
		)	
			.setSpec( \move, PointSpec( 200, 0.1 ) )
			.setSpec( \center, BoolSpec(false) )
			.useSelection_( true );
			
		WFSPathTransformerDef( \scale, 
			{ |f, path| path.positions_( path.positions * [f.get( \scale ) ] ); }, 
			[ \scale, 1@1 ] 
		)	
			.setSpec( \scale, PointSpec( 10, 0.1 ) )
			.useSelection_( true );
				
		WFSPathTransformerDef( \rotate,  
			{ |f, path| 
				var rotate;
				rotate = (f.get( \rotate ).neg / 360) * 2pi;
				path.positions_( path.positions.collect(_.rotate(rotate)) );
			}, 
			[ \rotate, 0 ] 
		)
			.setSpec( \rotate, ControlSpec( -180, 180, \lin, 0, 1 ) )
			.useSelection_( true );
			
		WFSPathTransformerDef( \smooth, 
			{ |f, path| 
				var newPos, win, n, amt;
				n = (f.get( \window ) * path.positions.size).max(3);
				amt = f.get( \smooth );
				win = ({ |i| 
					i.linlin(0,(n-1).max(2),-0.5pi,1.5pi).sin.linlin(-1,1,0,1) 
				}!n.max(2)).normalizeSum;
				newPos = path.positions.collect({ |item, i|
					var out, sum;	
					out = path.positions.modeAt(
						(i + (n/ -2).ceil .. i + (n/2).ceil - 1), path.clipMode ) * win;
					sum = 0@0;
					out.do({ |item| sum = sum + item; }); 
					sum;
				});
	 			
				path.positions_( 
					path.positions.collect({ |item, i| item.blend( newPos[i], amt ) })
				);
			}, 
			[ \smooth, 0, \window, 0.3 ],
			{ |f, path| [ \smooth, 0, \window, f.get( \window) ] }
		)
			.setSpec( \smooth, ControlSpec( -1, 1, \lin, 0, 0 ) )
			.setSpec( \window, ControlSpec( 0, 1, \lin, 0.1, 0.3 ) )
			.useSelection_( false );
			
		WFSPathTransformerDef( \size, 
			{ |f, path|
				var oldTimes;
				var newPos, newTimes;
				var mode, n;
				mode = f.get( \mode );
				n = f.get( \size ).asInt;
				switch( mode,
					\interpolate, {
						newTimes = [0] ++ path.times.integrate;
						newTimes = newTimes.resize( n, \linear, false );
						newPos = newTimes.collect({ |item| path.atTime( item ) });
						newTimes = newTimes.differentiate[1..];
					}, 
					\wrap, {
						oldTimes = path.times ++ [ path.times.last ];
						newPos = n.collect({ |i|
							path.positions.wrapAt( i ).copy;
						});
						newTimes = n.collect({ |i|
							oldTimes.wrapAt( i );
						});
						newTimes.pop;
						oldTimes.pop;
						newTimes = newTimes.normalizeSum( oldTimes.sum );
					},
					\fold, {
						oldTimes = path.times ++ [ path.times.last ];
						newPos = n.collect({ |i|
							path.positions.foldAt( i ).copy;
						});
						newTimes = n.collect({ |i|
							oldTimes.foldAt( i );
						});
						newTimes.pop;
						oldTimes.pop;
						newTimes = newTimes.normalizeSum( oldTimes.sum );
					
					}
				);
				path.positions_( newPos ).times_( newTimes );
			},
			[ \size, 10, \mode, \interpolate ],
			{ |f, path| [ \size, path.positions.size, \mode, f.get( \mode ) ] }
		)
			.setSpec( \size, PositiveIntegerSpec(2, 2) )
			.setSpec( \mode, ListSpec( [ \interpolate, \wrap, \fold ] ) )
			.useSelection_( false );
		
		WFSPathTransformerDef( \duration, 
			{ |f, path|
				var tms, origTms, seldur, dur, adddur;
				var sel;
				sel = f.selection;
				if( sel.size > 0 ) {
					origTms = path.times;
					tms = origTms.clipAt(sel);
					seldur = tms.sum;
					dur = path.times.sum;
					adddur = f.get( \duration ) - dur;
					tms = (tms.normalizeSum * (seldur + adddur)).max(0.001);
					tms.do({ |tm, i|
						origTms[ sel[i] ] = tm;
					});
					path.times = origTms;
				};
				path.times = path.times.normalizeSum * f.get( \duration );
			},
			[ \duration, 1 ],
			{ |f, path| [ \duration, path.duration ] }
		)
			.setSpec( \duration, SMPTESpec(0.001) )
			.useSelection_( false );
			
		WFSPathTransformerDef( \equal,
			{ |f, path|
				var oldTimes, deltas;
				var newPos, equalTimes, newTimes;
				var mode, amt;
				
				mode = f.get( \equal );
				amt = f.get( \amount );
				oldTimes = path.times;
				equalTimes = oldTimes.blend( 
					((1/oldTimes.size) * oldTimes.sum)!(oldTimes.size),
					amt 
				);
				
				switch( mode,
					\times, {
						newTimes = equalTimes;
					}, 
					\speeds, {
						deltas = path.positions[1..].collect({ |pos, i|
							pos.dist( path.positions[i] )
						}).normalizeSum;
						oldTimes = path.times;
						newTimes = oldTimes.blend( 
							deltas * oldTimes.sum,
							amt 
						);
						path.times = newTimes;
					}
				);
				
				if( f.get( \resample ) == true ) {
					
					newPos = ([0] ++ equalTimes.integrate).collect({ |item|
						path.atTime( item );
					});
					path.positions = newPos;
					newTimes = equalTimes;
				};
				newTimes = newTimes.max(0); // clip negative times
				path.times_( newTimes );
			},
			[ \equal, \times, \amount, 0, \resample, false ], 
			{ |f, path| [ \equal, f.get( \equal ), \amount, 0, \resample, f.get( \resample ) ] }
		)
			.setSpec( \equal, ListSpec( [ \times, \speeds ] ))
			.setSpec( \amount, ControlSpec(-1,1,\lin,0,0) )
			.setSpec( \resample, BoolSpec( false ) )
			.useSelection_( false );
				
		WFSPathTransformerDef( \reverse, 
				{ |f, path|
				if( f.get( \reverse ) ) {
					path.positions = path.positions.reverse;
					path.times = path.times.reverse;
					path;
				} {
					path;
				};
			},
			[ \reverse, false ],
			{ |f, path| [ \reverse, nil ] } // never bypass
		)	
			.setSpec( \reverse, BoolSpec(false ) );
			
			
		WFSPathTransformerDef( \simpleSize, 
			{ |f, path|
				var oldTimes;
				var newPos, newTimes;
				var n;
				n = f.get( \size ).asInt;
				newTimes = [0] ++ path.times.integrate;
				newTimes = newTimes.resize( n, \linear, false );
				newPos = newTimes.collect({ |item| path.atTime( item ) });
				newTimes = newTimes.differentiate[1..];
				path.positions_( newPos ).times_( newTimes );
			},
			[ \size, 10 ],
			{ |f, path| [ \size, path.positions.size ] }
		)
			.setSpec( \size, PositiveIntegerSpec(2, 2) )
			.useSelection_( false );

	
	}
}
	