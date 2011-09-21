WFSPathSpec : Spec {
	
	*testObject { |obj|
		^obj.isKindOf( WFSPathBuffer );
	}
	
	constrain { |value|
		^value;
	}
	
	default { 
		^WFSPathBuffer(  WFSPath2( { (8.0@8.0).rand2 } ! 7, [0.5] ) );
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

