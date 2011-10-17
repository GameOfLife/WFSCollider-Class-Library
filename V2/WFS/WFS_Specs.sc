WFSPathSpec : Spec {
	
	*testObject { |obj|
		^obj.isKindOf( WFSPathBuffer );
	}
	
	constrain { |value|
		^value;
	}
	
	default { 
		^WFSPathBuffer( WFSPath2( { (8.0@8.0).rand2 } ! 7, [0.5] ) );
	}
	
	*newFromObject { |obj|
		^this.new;
	}
	
	viewNumLines { ^WFSPathBufferView.viewNumLines }
	
	viewClass { ^WFSPathBufferView }
	
	makeView { |parent, bounds, label, action, resize|
		var vws, view, labelWidth;
		
		vws = ();
		
		bounds.isNil.if{bounds= 350 @ (this.viewNumLines * 18) };
		view = EZCompositeView( parent, bounds, gap: 4@4 );
		bounds = view.view.bounds;
		
		 vws[ \view ] = view;
		 
		if( label.notNil ) {
			labelWidth = (RoundView.skin ? ()).labelWidth ? 80;
			vws[ \labelView ] = StaticText( vws[ \view ], labelWidth @ 14 )
				.string_( label.asString ++ " " )
				.align_( \right )
				.resize_( 4 )
				.applySkin( RoundView.skin );
		} {
			labelWidth = -4;
		};
		
		if( resize.notNil ) { vws[ \view ].resize = resize };
		
		vws[ \wfsPathBufferView ] = this.viewClass.new( vws[ \view ], 
			( bounds.width - (labelWidth+4) ) @ bounds.height, { |vw|
				action.value( vw, vw.value )
			} )
		
		^vws;
	}
	
	setView { |view, value, active = false|
		view[ \wfsPathBufferView ].value = value;
		if( active ) { view.doAction };
	}
	
}

WFSPointSpec : PointSpec {
	
	makeView { |parent, bounds, label, action, resize|
		var vws, view, labelWidth;
		var localStep;
		var modeFunc;
		var font;
		var editAction;
		var tempVal;
		vws = ();
		
		font =  (RoundView.skin ? ()).font ?? { Font( Font.defaultSansFace, 10 ); };
		
		localStep = step.copy;
		if( step.x == 0 ) { localStep.x = 1 };
		if( step.y == 0 ) { localStep.y = 1 };
		bounds.isNil.if{bounds= 320@20};
		
		view = EZCompositeView( parent, bounds, gap: 4@4 );
		bounds = view.asView.bounds;
				
		vws[ \view ] = view;
		
		vws[ \val ] = 0@0;
		 		
		if( label.notNil ) {
			labelWidth = (RoundView.skin ? ()).labelWidth ? 80;
			vws[ \labelView ] = StaticText( vws[ \view ], labelWidth @ bounds.height )
				.string_( label.asString ++ " " )
				.align_( \right )
				.resize_( 4 )
				.applySkin( RoundView.skin );
		} {
			labelWidth = 0;
		};
		
		vws[ \comp1 ] = CompositeView( view, 40 @ (bounds.height) );
		
		vws[ \xy ] = XYView( view, bounds.height.asPoint )
			.action_({ |xy|
				var newVal, theta;
				tempVal = tempVal ?? { vws[ \val ].copy };
				newVal = tempVal + (xy.value * localStep * (1 @ -1));
				this.setView( vws, newVal );
				action.value( vws, newVal );
			})
			.mouseUpAction_({
				tempVal = nil;
			});
			
		vws[ \comp2 ] = CompositeView( view, 60 @ (bounds.height) );
		
		vws[ \mode ] = PopUpMenu( view, 60 @ (bounds.height) )
			.font_( font )
			.applySkin( RoundView.skin ? () )
			.items_([ 'point', 'polar', 'deg_cw' ])
			.action_({ |pu|
				mode = pu.item;
				this.setMode( vws, mode );
			});
		
		// point mode
		vws[ \x ] = SmoothNumberBox( vws[ \comp1 ], 40 @ (bounds.height) )
			.action_({ |nb|
				vws[ \val ] = nb.value @ vws[ \val ] .y;
				this.setView( vws, vws[ \val ]  );
				action.value( vws, vws[ \val ] );
			})
			//.step_( localStep.x )
			.scroll_step_( localStep.x )
			.clipLo_( rect.left )
			.clipHi_( rect.right )
			.value_(0);
			
		vws[ \y ] = SmoothNumberBox( vws[ \comp2 ], 40 @ (bounds.height) )
			.action_({ |nb|
				vws[ \val ]  = vws[ \val ] .x @ nb.value;
				this.setView( vws, vws[ \val ]  );
				action.value( vws, vws[ \val ]  );
			})
			//.step_( localStep.y )
			.scroll_step_( localStep.y )
			.clipLo_( rect.top )
			.clipHi_( rect.bottom )
			.value_(0);
		
		// polar, deg_cw
		vws[ \rho ] = SmoothNumberBox( vws[ \comp1 ], 40 @ (bounds.height) )
			.action_({ |nb|
				vws[ \val ]  = vws[ \val ] .asPolar.rho_( nb.value ).asPoint;
				this.setView( vws, vws[ \val ]  );
				action.value( vws, vws[ \val ]  );
			})
			//.step_( localStep.x )
			.scroll_step_( localStep.x )
			.clipLo_( 0 )
			.clipHi_( rect.height.max( rect.width ) )
			.value_(0);
		
		// polar	
		// -pi - pi counterclockwise (0 = straight right)
		vws[ \theta ] = SmoothNumberBox( vws[ \comp2 ], 40 @ (bounds.height) )
			.action_({ |nb|
				vws[ \val ]  = vws[ \val ] .asPolar.theta_( nb.value * pi ).asPoint;
				this.setView( vws, vws[ \val ]  );
				action.value( vws, vws[ \val ]  );
			})
			.step_( 0.25 )
			.scroll_step_( 0.005 )
			.clipLo_( -1 )
			.clipHi_( 1 )
			.wrap_(true)
			.value_(0);
			
		vws[ \thetaLabel ] = StaticText( vws[ \comp2 ], Rect( 42, 0, 18, bounds.height) )
			.applySkin( RoundView.skin ? () )
			.string_( "pi" );
		
		// deg_cw	
		// 0 - 360 clockwise (0 = straight front)
		vws[ \deg_cw ] = SmoothNumberBox( vws[ \comp2 ], 40 @ (bounds.height) )
			.action_({ |nb|
				vws[ \val ]  = vws[ \val ] .asPolar.theta_( 
					nb.value.neg.linlin(-180,180,-0.5pi,1.5pi)
				).asPoint;
				this.setView( vws, vws[ \val ]  );
				action.value( vws, vws[ \val ]  );
			})
			.step_( 1 )
			.scroll_step_( 1 )
			.clipLo_( -180 )
			.clipHi_( 180 )
			.wrap_(true)
			.value_(0);
			
		editAction = { |vw|
			this.setView( vws, vw.object[0].copy );
			action.value( vws, vws[ \val ]  );
		};
			
		vws[ \edit ] = SmoothButton( view, 40 @ (bounds.height) )
			.label_( "edit" )
			.border_( 1 )
			.radius_( 2 )
			.font_( font )
			.action_({
				var editor;
				if( vws[ \editor ].isNil or: { vws[ \editor ].isClosed } ) {
					editor = WFSPointView( object: [ vws[ \val ] ] )
						.canChangeAmount_( false )
						.action_( editAction )
						.onClose_({ 
							if( vws[ \editor ] == editor ) {
								vws[ \editor ] = nil;
							};
						});
					vws[ \editor ] = editor;
				} {
					vws[ \editor ].front;
				};
				
			});
			
		view.view.onClose_({
			if( vws[ \editor ].notNil ) {
				vws[ \editor ].close;
			};
		});
			
		this.setMode( vws, mode );
	
		^vws;
	}
	
	setMode { |view, newMode|
		switch( newMode,
			\point, {
				[ \x, \y ].do({ |item|
					view[ item ].visible = true;
				});
				[ \rho, \theta, \thetaLabel, \deg_cw ].do({ |item|
					view[ item ].visible = false;
				});
			},
			\polar, {
				[ \rho, \theta, \thetaLabel ].do({ |item|
					view[ item ].visible = true;
				});
				[ \x, \y, \deg_cw ].do({ |item|
					view[ item ].visible = false;
				});
			},
			\deg_cw, {
				[ \rho, \deg_cw, \thetaLabel ].do({ |item|
					view[ item ].visible = true;
				});
				[ \x, \y, \theta, \thetaLabel ].do({ |item|
					view[ item ].visible = false;
				});
			}
		);
	}
	
	setView { |view, value, active = false|
		var constrained, theta;
		constrained = this.constrain( value );
		theta = constrained.theta;
		view[ \val ] = value;
		view[ \x ].value = constrained.x;
		view[ \y ].value = constrained.y;
		view[ \rho ].value = constrained.rho;
		view[ \theta ].value = theta / pi;
		view[ \deg_cw ].value = theta
			.wrap( -0.5pi, 1.5pi)
			.linlin(-0.5pi, 1.5pi, 180, -180 );
		view[ \editor ] !? {
			view[ \editor ].object[ 0 ] = value;
			view[ \editor ].refresh;
		};
		this.setMode( view, mode );
		{ view[ \mode ].value = view[ \mode ].items.indexOf( mode ) ? 0; }.defer;
		if( active ) { view[ \x ].doAction };
	}
	
	mapSetView { |view, value, active = false|
		this.setView( view, this.map( value ), active );
	}
	
	massEditSpec { |inArray|
		^WFSMultiPointSpec( rect, step, inArray, units, mode ); 
	}
	
	massEditValue { |inArray|
		^inArray
	}
	
	massEdit { |inArray, params|
		^params;
	}

}

WFSMultiPointSpec : PointSpec {
	
	// array of points instead of a single point
	
	*testObject { |obj|
		^obj.isCollection && { obj[0].class == Point };
	}
	
	constrain { |value|
		^value.collect(_.clip( clipRect.leftTop, clipRect.rightBottom )); //.round( step );
	}
	
	map { |value|
		^this.constrain( value.linlin(0, 1, rect.leftTop, rect.rightBottom, \none ) );
	}
	
	unmap { |value|
		^this.constrain( value ).linlin( rect.leftTop, rect.rightBottom, 0, 1, \none );
	}
	
	makeView { |parent, bounds, label, action, resize|
		var vws, view, labelWidth;
		var localStep;
		var font;
		var editAction;
		vws = ();
		
		font =  (RoundView.skin ? ()).font ?? { Font( Font.defaultSansFace, 10 ); };
		
		localStep = step.copy;
		if( step.x == 0 ) { localStep.x = 1 };
		if( step.y == 0 ) { localStep.y = 1 };
		bounds.isNil.if{bounds= 320@20};
		
		view = EZCompositeView( parent, bounds, gap: 4@4 );
		bounds = view.asView.bounds;
		
		vws[ \view ] = view;
		
		vws[ \val ] = this.default ? [];
		
		if( label.notNil ) {
			labelWidth = (RoundView.skin ? ()).labelWidth ? 80;
			vws[ \labelView ] = StaticText( vws[ \view ], labelWidth @ bounds.height )
				.string_( label.asString ++ " " )
				.align_( \right )
				.resize_( 4 )
				.applySkin( RoundView.skin );
		} {
			labelWidth = 0;
		};

				
		editAction = { |vw|
			vws[ \val ] = vw.object;
			action.value( vws, vws[ \val ] );
		};
		
		vws[ \edit ] = SmoothButton( view, 40 @ (bounds.height) )
			.label_( "edit" )
			.border_( 1 )
			.radius_( 2 )
			.font_( font )
			.action_({
				var editor;
				if( vws[ \editor ].isNil or: { vws[ \editor ].isClosed } ) {
					editor = WFSPointView( object: vws[ \val ] )
						.canChangeAmount_( false )
						.action_( editAction )
						.onClose_({ 
							if( vws[ \editor ] == editor ) {
								vws[ \editor ] = nil;
							};
						});
					vws[ \editor ] = editor;
				} {
					vws[ \editor ].front;
				};
				
			});
			
		view.view.onClose_({
			if( vws[ \editor ].notNil ) {
				vws[ \editor ].close;
			};
		});
	
		^vws;
	}
	
	setView { |view, value, active = false|
		view[ \val ] = value.deepCopy;
		view[ \editor ] !? {
			view[ \editor ].object_( value, active ); 
		};
	}
	
}

WFSRectSpec : RectSpec {
	
	viewNumLines { ^2 }
	
	makeView { |parent, bounds, label, action, resize|
		var vws, view, labelWidth, val, wh;
		var localStep, setCenter, setWH, editAction, setEditor;
		var font, subView;
		vws = ();
		
		font = Font( Font.defaultSansFace, 10 );
		
		localStep = 0.01@0.01;
		
		vws[ \rect ] = this.default;
		
		setCenter = { |center|
			vws[ \rect ] = vws[ \rect ].center_( center );
		};
		
		setWH = { |whx|
			vws[ \rect ].centeredExtent_( whx );
		};
		
		setEditor = { |val|
			vws[ \editor ] !? {
				vws[ \editor ].object = val ? vws[ \rect ];
				vws[ \editor ].refresh;
			};
		};
		
		bounds.isNil.if{bounds= 320@40};
		
		#view, bounds = EZGui().prMakeMarginGap.prMakeView( parent, bounds );
		 vws[ \view ] = view;
		 
		view.addFlowLayout( 0@0, 4@4 );
		
		bounds.height = (bounds.height / 2) - 2;
		 		
		if( label.notNil ) {
			labelWidth = (RoundView.skin ? ()).labelWidth ? 80;
			vws[ \labelView ] = StaticText( vws[ \view ], labelWidth @ bounds.height )
				.string_( label.asString ++ " " )
				.align_( \right )
				.resize_( 4 )
				.applySkin( RoundView.skin );
		} {
			labelWidth = 0;
		};
		
		/////// center
		
		vws[ \centerLabel ] = StaticText( vws[ \view ], 36 @ bounds.height )
			.string_( "center " )
			.align_( \right )
			.font_( font )
			.applySkin( RoundView.skin );
		
		
		vws[ \x ] = SmoothNumberBox( vws[ \view ], 40 @ bounds.height )
			.action_({ |nb|
				setCenter.value( nb.value @ vws[ \y ].value );
				val = vws[ \rect ].center;
				setEditor.value;
				action.value( vws, vws[ \rect ]);
			})
			//.step_( localStep.x )
			.scroll_step_( localStep.x )
			.clipLo_( rect.left )
			.clipHi_( rect.right )
			.value_(0);
			
		vws[ \xy ] = XYView( vws[ \view ],  bounds.height @ bounds.height )
			.action_({ |xy|
				val = val ?? { vws[ \rect ].center };
				vws[ \x ].value = (val.x + (xy.x * localStep.x))
					.clip( rect.left, rect.right );
				vws[ \y ].value = (val.y + (xy.y * localStep.y.neg))
					.clip( rect.top, rect.bottom );
				setCenter.value( val + (xy.value * localStep * (1 @ -1) ) );
				setEditor.value;
				action.value( vws, vws[ \rect ] );
			})
			.mouseUpAction_({
				val = vws[ \x ].value @ vws[ \y ].value;
			});
			
		vws[ \y ] = SmoothNumberBox( vws[ \view ], 40 @ bounds.height )
			.action_({ |nb|
				setCenter.value( vws[ \x ].value @ nb.value );
				setEditor.value;
				val = vws[ \rect ].center;
				action.value( vws, vws[ \rect ] );
			})
			//.step_( localStep.y )
			.scroll_step_( localStep.y )
			.clipLo_( rect.top )
			.clipHi_( rect.bottom )
			.value_(0);
			
		////// edit button
		
		editAction = { |vw|
			this.setView( vws, vw.object.copy, includeEditor: false );
			val = vws[ \rect ].center;
			wh = vws[ \rect ].extent;
			action.value( vws, vws[ \rect ]  );
		};
			
		vws[ \edit ] = SmoothButton( view, 40 @ (bounds.height) )
			.label_( "edit" )
			.border_( 1 )
			.radius_( 2 )
			.font_( font )
			.action_({
				var editor;
				if( vws[ \editor ].isNil or: { vws[ \editor ].isClosed } ) {
					editor = WFSRectView( object: vws[ \rect ] )
						.action_( editAction )
						.onClose_({ 
							if( vws[ \editor ] == editor ) {
								vws[ \editor ] = nil;
							};
						});
					vws[ \editor ] = editor;
				} {
					vws[ \editor ].front;
				};
				
			});
			
		view.onClose_({
			if( vws[ \editor ].notNil ) {
				vws[ \editor ].close;
			};
		});
		
		///////// width/height
		
		vws[ \view ].decorator.nextLine;
		
		if( labelWidth > 0 ) {
			vws[ \view ].decorator.shift( labelWidth + 4, 0 );
		};
		
		vws[ \whLabel ] = StaticText( vws[ \view ], 36 @ bounds.height )
			.string_( "w / h " )
			.align_( \right )
			.font_( font )
			.applySkin( RoundView.skin );
			
		vws[ \width ] = 
			SmoothNumberBox( vws[ \view ], 40 @ bounds.height )
			.action_({ |nb|
				setWH.value( nb.value @ vws[ \height ].value );
				wh = vws[ \rect ].extent;
				setEditor.value;
				action.value( vws, vws[ \rect ]);
			})
			//.step_( localStep.x )
			.scroll_step_( localStep.x )
			.clipLo_( 0)
			.clipHi_( rect.right )
			.value_(10);
				
		vws[ \wh ] = XYView( vws[ \view ], bounds.height @ bounds.height )
			.action_({ |xy|
				wh = wh ?? { vws[ \rect ].extent };
				vws[ \width ].value = (wh.x + (xy.x * localStep.x))
					.clip( 0, rect.width );
				vws[ \height ].value = (wh.y + (xy.y * localStep.y.neg))
					.clip( 0, rect.height );
				setWH.value( (wh + (xy.value * localStep * (1 @ -1) )).clip(0@0, rect.extent) );
				setEditor.value;
				action.value( vws, vws[ \rect ] );
			})
			.mouseUpAction_({
				wh = vws[ \width ].value @ vws[ \height ].value;
			});

		vws[ \height ] = 
			SmoothNumberBox( vws[ \view ], Rect( labelWidth + 2, 0, 40, bounds.height ) )
			.action_({ |nb|
				setWH.value( vws[ \width ].value @ nb.value );
				setEditor.value;
				wh = vws[ \rect ].extent;
				action.value( vws, vws[ \rect ]);
			})
			//.step_( localStep.x )
			.scroll_step_( localStep.x )
			.clipLo_( 0 )
			.clipHi_( rect.right )
			.value_(10);
							
		^vws;
	}
	
	setView { |view, value, active = false, includeEditor = true|
		var constrained;
		constrained = this.constrain( value );
		view[ \rect ] = constrained.copy;
		view[ \x ].value = constrained.center.x;
		view[ \y ].value = constrained.center.y;
		view[ \width ].value = constrained.width;
		view[ \height ].value = constrained.height;
		view[ \editor ] !? {
			if( includeEditor == true ) {
				view[ \editor ].object = constrained;
				view[ \editor ].refresh;
			};
		};
		if( active ) { view[ \x ].doAction };
	}
	
	mapSetView { |view, value, active = false|
		var mapped;
		mapped = this.map( value );
		view[ \rect ] = mapped.copy;
		view[ \x ].value = mapped.center.x;
		view[ \y ].value = mapped.center.y;
		view[ \width ].value = mapped.width;
		view[ \height ].value = mapped.height;
		view[ \editor ] !? {
			view[ \editor ].object = mapped;
			view[ \editor ].refresh;
		};
		if( active ) { view[ \x ].doAction };
	}
	
}