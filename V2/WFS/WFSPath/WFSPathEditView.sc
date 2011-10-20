WFSPathEditView {
	
	var <object;
	var <objectCopy;
	var <>editFuncs;
	var <view, <views;
	var <>action;
	
	*new { |parent, bounds, object|
		^super.new.init( parent, bounds, object )
	}
	
	init { |parent, bounds, inObject|
		
		object = inObject ? object;
		object = object.asWFSPath2;
		objectCopy = object.deepCopy;
		
		
		if( parent.isNil ) {
			bounds = bounds ?? { 167 @ 280 };
		};
		
		view = EZCompositeView( parent, bounds, true, 2@2, 2@2 );
		editFuncs = this.makeEditFuncs;
		
		this.makeViews;
		
		view.view.decorator.nextLine;
		
		view.decorator.shift( 55, 0 );
		
		views[ \apply ] = SmoothButton( view, 40@14 )
			.font_( Font( Font.defaultSansFace, 10 ) )
			.label_( "apply" )
			.border_( 1 )
			.radius_( 2 )
			.action_({ 
				this.apply( true );
				action.value( this, \apply )
			});
			
		views[ \reset ] = SmoothButton( view, 40@14 )
			.font_( Font( Font.defaultSansFace, 10 ) )
			.label_( "reset" )
			.border_( 1 )
			.radius_( 2 )
			.action_({ 
				this.reset;
				action.value( this, \reset );
			});
		
		view.view.bounds = view.view.bounds.height_( view.view.children.last.bounds.bottom );
		
		this.resetFuncs;
		
	}
	
	selected {
		^editFuncs[1].selection;
	}
	
	selected_ { |selected|
		editFuncs.clump(2).flop[1].do( _.selection_(selected) );
	}
	
	revertObject { 
		object.positions = objectCopy.positions;
		object.times = objectCopy.times;
		object.type = objectCopy.type;
		object.curve = objectCopy.curve;
		object.clipMode = objectCopy.clipMode;
	}
	
	object_ { |newObject|
		object = newObject;
		this.resetFuncs;
		objectCopy = object.deepCopy;
	}
	
	resize_ { |resize|
		view.resize_( resize )
	}
	
	apply { |final = true, active = false|
		
		if( this.checkBypass.not ) {	
			this.revertObject;
			
			editFuncs.pairsDo({ |key, func|
				func.value( object );
			});
			
			if( final ) {
				this.resetFuncs;
				objectCopy = object.deepCopy;
				this.changed( \apply );
			};
		};
	
		^object;
	}
	
	resetFuncs {
		editFuncs.pairsDo({ |key, func|
			func.reset( object );
		});
	}
	
	checkBypass { 
		var bypass = true;
		editFuncs.pairsDo({ |key, value|
			if( value.checkBypass( object ).not ) {
				bypass = false;
			};
		});
		^bypass;
	}
	
	reset {
		this.revertObject;
		this.resetFuncs;
		this.changed( \reset );
	}
	
	makeViews {
		views = ();
		
		editFuncs.clump(2).collect({ |item|
			var key, func;
			#key, func = item;
			func.action = { 
				this.apply( false );
				action.value( this, key );
			};
			views[ key ] = func.makeViews( view, view.bounds );
			view.view.decorator.nextLine;
		});
		 
	}
	
	makeEditFuncs { 
		^[
			\name, WFSPathEditFunc( 
			 	{ |f, obj| obj.name_( f.get( \name ) ) }, 
			 	[ \name, "" ], 
			 	{ |f, obj| [ \name, obj.name ] } 
			 )	
			 	.setSpec( \name, StringSpec() )
				.useSelection_( false ),
				
			\type, WFSPathEditFunc( 
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
			 	.useSelection_( false ),
			 	
			\move, WFSPathEditFunc( 
				{ |f, path| path.positions_( path.positions + [f.get( \move )] ); }, 
				[ \move, 0@0 ] 
			)	
				.setSpec( \move, PointSpec( 200, 0.1 ) )
				.useSelection_( true ),
				
			\scale, WFSPathEditFunc( 
				{ |f, path| path.positions_( path.positions * [f.get( \scale ) ] ); }, 
				[ \scale, 1@1 ] 
			)	
				.setSpec( \scale, PointSpec( 10, 0.1 ) )
				.useSelection_( true ),
				
			\rotate, WFSPathEditFunc( 
				{ |f, path| 
					var rotate;
					rotate = (f.get( \rotate ) / 360) * 2pi;
					path.positions_( path.positions.collect(_.rotate(rotate)) );
				}, 
				[ \rotate, 0 ] 
			)
				.setSpec( \rotate, ControlSpec( -180, 180, \lin, 0, 1 ) )
				.useSelection_( true ),
			
			\smooth, WFSPathEditFunc( 
				{ |f, path| 
					var newPos, win, n, amt;
					n = (f.get( \order ) * path.positions.size).max(3);
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
				[ \smooth, 0, \order, 0.3 ],
				{ |f, path| [ \smooth, 0, \order, f.get( \order) ] }
			)
				.setSpec( \smooth, ControlSpec( -1, 1, \lin, 0, 0 ) )
				.setSpec( \order, ControlSpec( 0, 1, \lin, 0.1, 0.3 ) )
				.useSelection_( false ),
			
			\size, WFSPathEditFunc( 
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
								path.positions.wrapAt( i );
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
								path.positions.foldAt( i );
							});
							newTimes = n.collect({ |i|
								oldTimes.wrapAt( i );
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
				.useSelection_( false ),
		
			\duration, WFSPathEditFunc( 
				{ |f, path|
					path.times = path.times.normalizeSum * f.get( \duration );
				},
				[ \duration, 1 ],
				{ |f, path| [ \duration, path.duration ] }
			)
				.setSpec( \duration, SMPTESpec(0.001) )
				.useSelection_( false ),
			
			\equal, WFSPathEditFunc( 
				{ |f, path|
					var oldTimes, deltas;
					var newPos, equalTimes, newTimes;
					var mode, amt;
					
					mode = f.get( \equal );
					amt = f.get( ' ' );
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
						
					path.times_( newTimes );
				},
				[ \equal, \times, ' ', 0, \resample, false ], 
				{ |f, path| [ \equal, f.get( \equal ), ' ', 0, \resample, f.get( \resample ) ] }
			)
				.setSpec( \equal, ListSpec( [ \times, \speeds ] ))
				.setSpec( ' ', ControlSpec(0,1,\lin,0,0) )
				.setSpec( \resample, BoolSpec( false ) )
				.useSelection_( false ),
				
			\reverse, WFSPathEditFunc( 
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
				.setSpec( \reverse, BoolSpec(false ) )
				
		
		];
	}
	
	
}