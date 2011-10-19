//////// COMBINED TIME AND XY EDITOR ////////////////////////////////////////////////////

WFSPathView {
	
	var <view, <xyView, <timeView, <topBar, <undoView, <undoManager;
	
	*new { |parent, bounds, object, addUndoManager = true|
		^super.new.init( parent, bounds, object )
			.addUndoManager( addUndoManager );
	}
	
	init { |parent, bounds, object|
		
		if( parent.isNil ) { 
			bounds = bounds ?? { 420 @ 516 }; 
		} {
			bounds = bounds ? parent.asView.bounds;
		};
		
		view = EZCompositeView( parent, bounds, gap: 2@2, margin: 2@2 );
		view.resize_(5);
		bounds = view.asView.bounds;
		view.addFlowLayout(0@0, 2@2);
		
		topBar = WFSPathView_TopBar( view, 16 );
		undoView = UndoView( view, 16 );
		
		view.view.decorator.nextLine;
		view.view.decorator.shift( 0, 2 ); // needed for some reason
		
		xyView = WFSPathXYView( view, bounds.copy.height_( bounds.height - 118 ), object );
		
		topBar.object = xyView;
		undoView.object = xyView;
		
		view.view.decorator.shift( 0, 2 );
		
		timeView = WFSPathTimeView( view, bounds.copy.height_( 92 ), object ? xyView.object );
		timeView.resize_(8);
		
		xyView.addDependant({ this.updateTimeView });
		timeView.addDependant({ this.updateXYView });
	}
	
	updateXYView {
		xyView.select( timeView.selected );
		xyView.pos_( timeView.pos, false );
		{ xyView.refresh; }.defer;
	}
	
	updateTimeView {
		timeView.select( xyView.selected );
		timeView.pos_( xyView.pos, false );
		{ timeView.refresh; }.defer;
	}
	
	addUndoManager { |bool = true|
		if( bool ) { this.undoManager = UndoManager() };
	}
	
	undoManager_ { |um|
		xyView.undoManager = um;
		timeView.undoManager_( um, false );
		undoView.undoManager = um;
	}
	
	undo { |...args|
		xyView.undo( *args );
	}
	
	refresh { 
		xyView.refresh;
		timeView.refresh;
	}
	
	object { ^xyView.object; }
	
	object_ { |new, update = true|
		timeView.object_( new, false );
		xyView.object_( new, update );
	}
	
	path { ^this.object }
	
	path_ { |pth, update = true|
		this.object_( pth, update );
	}
	
	resize_ { |resize|
		view.resize_(resize);
	}
	
	mouseMode_ { |mode|
		xyView.mouseMode = mode;
		timeView.mouseMode = mode;
	}
	
	editMode_ { |mode|
		xyView.editMode = mode;
		timeView.editMode = mode;
	}
	
	doesNotUnderstand { |selector ...args|
		var res;
		res = xyView.perform( selector, *args );
		if( res != xyView ) { ^res }
	}
	
}


WFSPathView_TopBar {
	
	classvar <>icons;
	
	var <object, <ctrl;
	var <view, <views;
	
	
	*new { |parent, bounds, object|
		^super.newCopyArgs( object ).makeView( parent, bounds );
		
	}
	
	*initClass {
		icons = 	(
			\select: { |bt, rect|  // \select
				//var scaleAmt;
				
				
				Pen.width = 1;
				Pen.lineDash_( FloatArray[ 1,1 ] );
				Pen.addRect( rect.insetBy( rect.width * 0.25,  rect.height * 0.25 ) );
				Pen.stroke;
			}, 
			\move: { |bt, rect|  // \move
				Pen.width = 0.8;
				4.do({ |i|
					Pen.arrow( rect.center, 
						rect.center + Polar( rect.height * 0.35, i * 0.5pi ).asPoint, 
						rect.height / 6 );
				});
				Pen.stroke;
			}, 
			\zoom: { |bt, rect|  // \zoom
				Pen.width = 1;
				rect = rect.insetBy( rect.width * 0.25,  rect.height * 0.25 );
				Pen.addOval(
					rect.insetBy( rect.width * 0.125,  rect.height * 0.125 )
				 		.moveBy(  rect.width * 0.25,  rect.height * -0.25 ) 
				 	);
				Pen.line( rect.center, rect.leftBottom );
				Pen.stroke;
			},
			\record: { |bt, rect|  // \record
				Pen.color = Color.red(0.75);
				DrawIcon( \record, rect );
			},
			\play: { |bt, rect|  // \record
				Pen.color = Color.green(0.5);
				DrawIcon( \play, rect );
			}
		);	
	}
	
	setMouseModeViews { |mode|
		var modes;
		modes = [ \select, \move, \zoom, \record ];
		
		modes.do({ |item|
			if( item === mode ) {
				views[ item ].value = 1;
			} {
				views[ item ].value = 0;
			};
		});
	}
	
	setEditModeView { |mode|
		{ 
			views[ \editMode ].value = views[ \editMode ].items.indexOf( mode );
		}.defer;
	}
	
	object_ { |newObj|
		
		ctrl.remove;
		
		object = newObj;
		
		ctrl = SimpleController( object )
			.put( \mouseMode, { this.setMouseModeViews( object.mouseMode ) })
			.put( \editMode, { this.setEditModeView( object.editMode ) })
			.put( \animate, { |obj, what, value|
				if( value == true ) { 
					views[ \play ].value = 1 
				} { 
					views[ \play ].value = 0 
				};
			})
			.put( \pos, { 
				views[ \pos ].value = (object.pos ? 0).linlin( 0, object.object.dur, 0, 1 );
			});
		
		this.setMouseModeViews( object.mouseMode );
		this.setEditModeView( object.editMode );	
	}
	
	makeView { |parent, bounds|
		
		var font, ctrl;
		var height = 16;
		
		if( bounds.isNumber ) { height = bounds; bounds = nil };
		
		bounds = bounds ?? { (((height + 2) * 5) + ((60 + 2) * 2)) @ height };
	
		view = CompositeView( parent, bounds );
		view.addFlowLayout( 0@0, 2@2 );
		
		views = ();
		
		bounds = view.drawBounds;
		
		font = Font( Font.defaultSansFace, 10 );
		
		views[ \editMode ] = PopUpMenu( view, 60 @ bounds.height )
			.font_( font )
			.canFocus_( false )
			.items_( [ \move, \scale, \rotate, \rotateS, \none ] )
			.action_({ |pu|
				object.editMode = pu.item;
			}); 
		
		// mouse modes
		[ \select, \move, \zoom, \record ].do({ |item|
			views[ item ] = SmoothButton( view, bounds.height @ bounds.height )
				.radius_(2)
				.border_(1)
				.canFocus_( false )
				.states_([
					[ icons[item], Color.black ],
					[ icons[item], Color.white, Color.gray(0.4) ]
				])
				.action_({ |bt|
					this.setMouseModeViews( item );
					object.mouseMode = item;
				});
		});
		
		views[ \play ] = SmoothButton( view, bounds.height @ bounds.height )
			.radius_(2)
			.border_(1)
			.canFocus_( false )
			.states_([
				[ icons[\play], Color.black ],
				[ icons[\play], Color.white, Color.gray(0.4) ]
			])
			.action_({ |bt|
				switch( bt.value.asInt,
					1, { object.animate( true ) },
					0, { object.animate( false ) }
				);
			});
			
		views[ \pos ] = SmoothSlider( view, 60@16 )
				.knobSize_(0)
				.thumbSize_(0)
				.border_(1)
				.borderColor_(Color.gray(0.25) )
				.baseWidth_(1)
				.canFocus_(false)
				.hilightColor_( Color.gray(0.2).alpha_(0.5) )
				.action_({ |sl|
					object.pos = sl.value.linlin( 0, 1, 0, object.object.dur );
				})
				.mouseUpAction_({ |sl|
					if( views[\play].value != 1 ) { 
						object.animate( false );
					};
				});
			
		view.onClose_({ ctrl.remove });	
	}
}