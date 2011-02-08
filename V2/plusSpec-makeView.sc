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

+ PointSpec {
	makeView {
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