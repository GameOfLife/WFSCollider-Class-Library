RadiationPatternSpec : Spec {
	
	var <>default;
	
	*new { |default = #[0,1,0,1]|
		^super.newCopyArgs( default );
	}
	
	*testObject { |obj|
		^(obj.isKindOf( ArrayedCollection ) ) && (obj.size == 4);
	}
	
	constrain { |value|
		value = value.asCollection;
		if( value.size != 4 ) {
			value = value.extend( 4, 0 );
		};
		^value.clip([0,0,0,1], [1, 1, 1, 8]).round([0,0,0,1]);
	}
	
	map { |value|
		^this.constrain( value );
	}
	
	unmap { |value|
		^this.constrain( value );
	}
	
	storeArgs {
	    ^[ default ]
	}
	
	// views
	
	viewNumLines { ^4 }
	
	makeView { |parent, bounds, label, action, resize|
		var view, vws, stp, labelWidth;
		var subViewBounds, subViewHeight;
		vws = ();
		
		vws[ \val ] = default.copy;
		
		view = EZCompositeView( parent, bounds );
		vws[ \view ] = view.view;
		bounds = view.view.bounds;
		
		if( label.notNil ) {
			labelWidth = (RoundView.skin ? ()).labelWidth ? 80;
			vws[ \labelView ] = StaticText( view, labelWidth @ 14 )
				.string_( label.asString ++ " " )
				.align_( \right )
				.resize_( 4 )
				.applySkin( RoundView.skin );
		} {
			labelWidth = -4;
		};
		
		subViewHeight = bounds.height / 4;
		subViewBounds = Rect(
			labelWidth + 4,
			0,
			bounds.width-(labelWidth + 4),
			subViewHeight - 2
		);
		
		[ \omni, \dipole, \quadrupole ].do({ |name, i|
			vws[ name ] = EZSmoothSlider( view, 
				subViewBounds + Rect(0, i * subViewHeight, 0, 0 ),
				name.asString[0].asString, 
				[0,1].asSpec, 
				{ |vw|
					vws[ \val ][ i ] = vw.value;
		        		action.value( vws, vws[ \val ] );
		    		},
		    		vws[ \val ][ i ]
			).labelWidth_( 10 );
			vws[ name ].view.resize = 2;
			view.view.decorator.nextLine;
			view.view.decorator.shift( labelWidth + 2 );
		});
		
		vws[ \n ] = EZSmoothSlider( view, 
			subViewBounds + Rect(0, subViewHeight * 3, 0, 0 ),
			"n", 
			[1,8,\lin,1,1].asSpec, 
			{ |vw|
				vws[ \val ][ 3 ] = vw.value;
	        		action.value( vws, vws[ \val ] );
	    		},
	    		vws[ \val ][ 3 ]
		).labelWidth_( 10 );
		vws[ \n ].view.resize = 2;
		^vws;	
	}
	
	setView { |vws, value, active = false|
		[ \omni, \dipole, \quadrupole, \n ].do({ |name, i|
			vws[ name ].value = value[i];
		});
		if( active ) { vws[ \omni ].doAction };
	}
	
	mapSetView { |vws, value, active = false|
		value = this.constrain( value );
		[ \omni, \dipole, \quadrupole, \n ].do({ |name, i|
			vws[ name ].value = value[i];
		});
		if( active ) { vws[ \omni ].doAction };
	}

}