ObjectViewDef {
	
	classvar <>all;
	classvar <>defaultMakeFunc, <>defaultSetFunc;
	
	var >makeFunc, >setFunc, <>numLines = 1;
	
	*initClass {
		this.all = IdentityDictionary();
		
		this.defaultMakeFunc = { |parent, bounds, label, spec, action|
			var vw = EZSmoothSlider( parent, bounds, label.asString ++ " ", spec.asSpec,
				{ |sl| action.value( sl.value ); });
			vw.view.resize_(2);
			vw;
		};
		
		this.defaultSetFunc = { |view, value| view.value = value; };
		
		this.new( \default ); // add a default 
		
		this.new( \Array, { |parent, bounds, label, spec, action| // assume size == 2
			var vw = EZSmoothRanger( parent, bounds, label.asString ++ " ", spec.asSpec,
				{ |sl| action.value( sl.value ); });
			vw.view.resize_(5);
			vw; } );
			
		this.new( \Float, { |parent, bounds, label, spec, action|
			var vw = EZSmoothSlider( parent, bounds, label.asString ++ " ", spec.asSpec,
				{ |sl| action.value( sl.value ); });
			vw.view.resize_(5);
			vw; } );
		
		this.new( \Integer, { |parent, bounds, label, spec, action|
			var vw = EZSmoothSlider( parent, bounds, label.asString ++ " ", 
					(spec ? [0,100,\lin,1]).asSpec,
				{ |sl| action.value( sl.value ); });
			vw.view.resize_(5);
			vw; } );
		
		this.new( \String, { |parent, bounds, label, spec, action|
			var vw, labelWidth = 60;
			if( RoundView.skin.notNil )
				{ labelWidth = RoundView.skin.labelWidth ?  labelWidth };
			vw = EZText( parent, bounds, label.asString ++ " ",
					{ |sl| action.value( sl.value ); },
					labelWidth: labelWidth );
			vw.applySkin( RoundView.skin );
			vw.view.resize_(5);
			vw; } );
	}
	
	*new { |name, makeFunc, setFunc, numLines|
		^super.newCopyArgs( makeFunc, setFunc, numLines ? 1 ).addToAll( name );
	}
	
	makeFunc { ^makeFunc ? this.class.defaultMakeFunc; }
	setFunc { ^setFunc ? this.class.defaultSetFunc; }
	
	addToAll { |name|
		this.class.all[ name ] = this;
	}
	
}

ObjectView {
	
	classvar <viewHeight = 16;
	
	var <object, <key, <parent, <def, <composite, <views; 
	var <>action;
		// views can be anything; i.e. the output of the makeFunc in the def
	
	*new { |parent, bounds, object, key, spec, defName, controller|
		^super.newCopyArgs( object, key ).init( parent, bounds, spec, defName, controller );	}
		
	init { |inParent, bounds, spec, defName, controller|
		var margin = 0;
		parent = inParent;
		if( parent.isNil ) { parent = Window( "%:%".format(object) ).front.decorate };
		defName = defName ?? { object.perform( key ).class.name };
		def = ObjectViewDef.all[ defName.asSymbol ];
		if( def.isNil ) { def = ObjectViewDef.all[ \default ] };
		if( bounds.isNil ) { 
			if( parent.asView.decorator.notNil )
				{ margin = parent.asView.decorator.margin.x; };
			bounds = parent.bounds.width - (2 * margin);
		};
		if( bounds.isNumber ) { bounds = (bounds@(def.numLines * viewHeight)) };
		this.makeView( bounds, spec, controller );
	}
	
	makeView { | bounds, spec, controller |
		var createdController = false, setter;
		
		controller = controller ?? { 
			createdController = true; 
			SimpleController( object ); 
		};
		
		composite = CompositeView( parent, bounds ).resize_(2);
		composite.onClose = { 
			controller.put( key, nil );
			if( createdController ) { controller.remove };
		};
		
		setter = key.asSetter;
		
		views = def.makeFunc.value( composite, bounds.asRect.moveTo(0,0), key, spec, 
				{ |value| 
					object.perform( key.asSetter, value ); 
					action.value( this, value ); 
				} );
				
		this.update;
		
		controller.put( key, { |obj, key, value| def.setFunc.value( views, value ) });
	}
	
	update { |obj, inKey, value|
		def.setFunc.value( views, object.perform( inKey ? key ) ); 
	}
	
	remove { composite.remove }
	
	resize_ { |val| composite.resize_(val); }
	
}