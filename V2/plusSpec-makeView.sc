+ Spec {
	adaptFromObject { ^this }
	
	viewNumLines { ^1 }
}


+ ControlSpec {
	makeView { |parent, bounds, label, action, resize|
		var vw = EZSmoothSlider( parent, bounds, label !? { label.asString ++ " " }, 
			this, { |vw| action.value( vw, vw.value ) } );
		if( resize.notNil ) { vw.view.resize = resize };
		^vw;	
	}
	
	setView { |view, value, active = false|
		view.value = value;
		if( active ) { view.doAction };
	}
	
	mapSetView { |view, value, active = false|
		view.value = this.map(value);
		if( active ) { view.doAction };
	}
	
	adaptFromObject { |object| // if object out of range; change range
		if( object.isArray ) {
			^this.asRangeSpec.adaptFromObject( object );
		} {
			if( object.inclusivelyBetween( minval, maxval ).not ) {
				^this.copy
					.minval_( minval.min( object ) )
					.maxval_( maxval.max( object ) )
			};
			^this;
		};
	}
}

+ ListSpec {
	
	makeView { |parent, bounds, label, action, resize|
		var multipleActions = action.size > 0;
		var vw;
		vw = EZPopUpMenu( parent, bounds, label !? { label.asString ++ " " }, 
			if( multipleActions ) {
				list.collect({ |item, i| 
					item.asSymbol -> { |vw| action[i].value( vw, list[i] ) };
				});
			} { list.collect({ |item, i| item.asSymbol -> nil })
			},
			initVal: defaultIndex
		);
		if( multipleActions.not ) {
			vw.globalAction = { |vw| action.value( vw, list[vw.value] ) };
		};
		vw.labelWidth = 60; // same as EZSlider
		vw.applySkin( RoundView.skin ); // compat with smooth views
		if( resize.notNil ) { vw.view.resize = resize };
		^vw
	}
	
	setView { |view, value, active = false|
		{  // can call from fork
			view.value = this.unmap( value );
			if( active ) { view.doAction };
		}.defer;
	}
	
	mapSetView { |view, value, active = false|
		{
			view.value = value;
			if( active ) { view.doAction };
		}.defer;
	}
	
	adaptFromObject { |object|
		if( list.any({ |item| item == object }).not ) {
			^this.copy.add( object )
		} {
			^this
		};
	}
	
	
}

+ BoolSpec {	
	
	makeView { |parent, bounds, label, action, resize| 
		var vws, view, labelWidth;
		vws = ();
		
		// this is basically an EZButton
		
		bounds.isNil.if{bounds= 160@20};
		
		#view, bounds = EZGui().prMakeMarginGap.prMakeView( parent, bounds );
		 vws[ \view ] = view;
		 		
		if( label.notNil ) {
			labelWidth = (RoundView.skin ? ()).labelWidth ? 60;
			vws[ \labelView ] = StaticText( vws[ \view ], labelWidth @ bounds.height )
				.string_( label.asString ++ " " )
				.align_( \right )
				.resize_( 4 )
				.applySkin( RoundView.skin );
		} {
			labelWidth = 0;
		};
		
		if( trueLabel.isNil && falseLabel.isNil ) {
			vws[ \buttonView ] = SmoothButton( vws[ \view ], 
					Rect( labelWidth + 2, 0, bounds.height, bounds.height ) )
				.label_( [ "", 'x' ] )
		} {	
			vws[ \buttonView ] = SmoothButton( vws[ \view ], 
					Rect( labelWidth + 2, 0, bounds.width-(labelWidth+2), bounds.height ) )
				.label_( [ falseLabel, trueLabel ] );
		};
		
		vws[ \buttonView ]
				.radius_( bounds.height / 8 )
				.value_( this.unmap( this.constrain( default ) ) )
				.action_({ |bt| action.value( vws, this.map( bt.value ) ) })
				.resize_( 5 );

		if( resize.notNil ) { vws[ \view ].resize = resize };
		^vws;
	}
	
	setView { |view, value, active = false|
		view[ \buttonView ].value = this.unmap( this.constrain( value ) );
		if( active ) { view[ \buttonView ].doAction };
	}
	
	mapSetView { |view, value, active = false|
		view[ \buttonView ].value = this.map(  value );
		if( active ) { view[ \buttonView ].doAction };
	}
}
	
+ PointSpec {

	makeView { |parent, bounds, label, action, resize|
		var vws, view, labelWidth;
		vws = ();
		
		// TODO
		bounds.isNil.if{bounds= 160@20};
		
		#view, bounds = EZGui().prMakeMarginGap.prMakeView( parent, bounds );
		 vws[ \view ] = view;
		 		
		if( label.notNil ) {
			labelWidth = (RoundView.skin ? ()).labelWidth ? 60;
			vws[ \labelView ] = StaticText( vws[ \view ], labelWidth @ bounds.height )
				.string_( label.asString ++ " " )
				.align_( \right )
				.resize_( 4 )
				.applySkin( RoundView.skin );
		} {
			labelWidth = 0;
		};
		
		vws[ \x ] = SmoothNumberBox( vws[ \view ], 
			Rect( labelWidth + 2, 0, 40, bounds.height ) ).value_(0);
			
		vws[ \xy ] = XYView( vws[ \view ],
			Rect( labelWidth + 2 + 42, 0, bounds.height, bounds.height ) );
			
		vws[ \y ] =  SmoothNumberBox( vws[ \view ], 
			Rect( labelWidth + 2 + 42 + bounds.height + 2, 0, 40, bounds.height ) ).value_(0);
		
		^vws;
	
		
	}

	setView{ |view, value, active = false|
	    view[\xy].value_(value)

	}
}

+ RangeSpec {
	makeView { |parent, bounds, label, action, resize|
		var vw = EZSmoothRanger( parent, bounds, label !? { label.asString ++ " " }, 
			this.asControlSpec, 
			{ |sl| sl.value = this.constrain( sl.value ); action.value(sl, sl.value) }
			).value_( this.default ); 
		// later incorporate rangeSpec into EZSmoothRanger
		if( resize.notNil ) { vw.view.resize = resize };
		^vw;		
	}
	
	adaptFromObject { |object|
		if( object.isArray.not ) {
			^this.asControlSpec.adaptFromObject( object );
		} {	
			if(  (object.minItem < minval) or: (object.maxItem > maxval) ) {
				^this.copy
					.minval_( minval.min( object.minItem ) )
					.maxval_( maxval.max( object.maxItem ) )
			};
			^this;
		};
	}

}

+ IntegerSpec {

	makeView { |parent, bounds, label, action, resize|
		var box, composite, labelWidth = 55;
		bounds = bounds.insetBy(1,1);
		composite = CompositeView(parent, bounds);
		StaticText(composite, Rect(0,0,labelWidth,bounds.height)).string_(label).align_(\right);
		box = IntegerNumberBox( composite, Rect(60,0,bounds.width-60,bounds.height))
		    .action_({ |vw|
		        action.value( vw, vw.value );
		    } );
		if( resize.notNil ) { composite.resize = resize };
		^box;
	}

	setView { |view, value, active = false|
		view.value = value;
		if( active ) { view.doAction };
	}

}

+ EQSpec {

    makeView { |parent, bounds, label, action, resize|
        ^EQGui(parent, bounds, label, action, resize)
    }

    setView{ |view, value, active = false|
        view.setValue(value)
    }
}

+ EZPopUpMenu {
	
	labelWidth { ^labelView !? { labelView.bounds.width } ? 0 }
	
	labelWidth_ { |width = 60|
		var delta;
		if( layout === \horz && { labelView.notNil } ) { // only for horizontal sliders
			delta = labelView.bounds.width - width;
			labelView.bounds = labelView.bounds.width_( width );
			widget.bounds = widget.bounds
				.width_( widget.bounds.width + delta )
				.left_( widget.bounds.left - delta );
		};
	}
}