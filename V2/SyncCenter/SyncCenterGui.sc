SyncCenterGui {
	var <window, <widgets;
	
	*new {
		^super.new.init;
	}
	
	init {
		var font = Font( Font.defaultSansFace, 11 ), masterText;		
		widgets = List.new;
		
		if( window.notNil ){
			if( window.isClosed.not) {
				widgets.do(_.remove);
				window.close
			};
			window = nil;
		};
		window = Window("Sync Center",Rect(300,300,400,400)).front;
		window.addFlowLayout;
		
		SmoothButton( window, 100@20  )
 			.states_( [[ "Sync", Color.black, Color.clear ]] )
 			.canFocus_(false)
			.radius_( 0 )
			.border_(1)
			.font_( Font( font.name, 10 ).boldVariant )
			.action_({ |bt|
				SyncCenter.remoteSync;
				bt.enabled = false;
				{ bt.enabled = true }.defer(1.1);	
			});
		
		widgets.add(SyncCenterStatusWidget(window,20));

		window.view.decorator.nextLine;
		
		SyncCenter.serverCounts.keys.do{ |server,i|
			var text, uv;
			text = StaticText(window,100@20).string_(server.name++":" );
			widgets.add(SyncCenterServerWidget(window,130@17,server));
						
			window.view.decorator.nextLine;
		};
		
		window.onClose_({ this.remove });
			
	}
	
	remove {
	}
}

SyncCenterStatusWidget{
	var <controller, <view;
	var <>red, <>green;
	
	*new{ |parent, size = 20|
		^super.new.init(parent,size)
	}
	
	init{ |parent,size|
		red = Color.red(0.7);
		green = Color.green(0.7);
		
		view = UserView(parent,size@size);
		this.update;
				
		SyncCenter.ready.addDependant(this);
		
		view.onClose_({ SyncCenter.ready.removeDependant(this); });
	}
	
	update {
		 {
			view.background_( if( SyncCenter.ready.value ) { green } { red } )
		}.defer
	}
	
	remove{
		 SyncCenter.ready.removeDependant(this); 
	}
}

SyncCenterServerWidget{
	var <controller, <view, <difView, <remoteCount;
	var <>red, <>green;
	
	*new{ |parent, bounds, server|
		if( true ) {
			^super.new.init(parent,bounds,server)
		}
	}
	
	init{ |parent,bounds,server| 
		red = Color.red(0.7);
		green = Color.green(0.7);
		
		this.remove;
		remoteCount = SyncCenter.serverCounts.at(server);
		
		view = RoundNumberBox( parent, bounds.asRect.resizeBy( -64, 0 ) );
		difView = RoundNumberBox( parent, 60 @ (bounds.asRect.height) );
		this.update;
			
		remoteCount.addDependant(this); // becomes a controller
		view.onClose_( { remoteCount.removeDependant(this); } );
	}
	
	update { 
		view.value_(remoteCount.value);
		view.background_( if( remoteCount.value != -1 ) { green } { red } );
		if( remoteCount.value != -1 ) { 
			difView.value = remoteCount.value - 
				SyncCenter.serverCounts[ SyncCenter.master ].value;
		};
	}
	
	remove{
		remoteCount.removeDependant(this);
	}
}

	
		