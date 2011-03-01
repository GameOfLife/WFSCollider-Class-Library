SyncCenterGui {
	classvar <window, <widgets;
	
	*new{
		var font = Font( Font.defaultSansFace, 11 ), masterText, widgets;		
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
		
		SmoothButton( window, 100@30  )
 			.states_( [[ "Sync", Color.black, Color.clear ]] )
 			.canFocus_(false)
			.radius_( 0 )
			.border_(1)
			.font_( Font( font.name, 10 ).boldVariant )
			.action_({ 
				SyncCenter.remoteSync			
			});
			
		StaticText(window,100@20).string_("SyncStatus:");
		widgets.add(SyncCenterStatusWidget(window,17));

		window.view.decorator.nextLine;
		
		SyncCenter.servers.do{ |server,i|
			var text, uv;
			text = StaticText(window,100@20).string_(server.name++":" );
			widgets.add(SyncCenterServerWidget(window,70@17,server));
						
			window.view.decorator.nextLine;
		};
		
		window.onClose_({ this.remove });
			
	}
	
	*remove{
		widgets.do(_.remove);
	}
}

SyncCenterStatusWidget{
	var <controller, <view;
	
	*new{ |parent, size = 20|
		^super.new.init(parent,size)
	}
	
	init{ |parent,size|
		var red = Color.red(0.7);
		var green = Color.green(0.7);
		view = UserView(parent,size@size).background_(if( SyncCenter.ready.value ) { green } { red });
		controller = Updater(SyncCenter.ready, { |ready|
			{
				view.background_( if( ready.value ) { green } { red } )
			}.defer
		})
	}
	
	remove{
		controller.remove
	}
}

SyncCenterServerWidget{
	var <controller, <view;
	
	*new{ |parent, bounds, server|
		if(SyncCenter.servers.includes(server) ) {
			^super.new.init(parent,bounds,server)
		}
	}
	
	init{ |parent,bounds,server| 
		var red = Color.red(0.7);
		var green = Color.green(0.7);
		var remoteCount = SyncCenter.serverCounts.at(server);
		
		view = RoundNumberBox(parent,bounds)
			.background_(if( remoteCount.value != -1 ) { green } { red})
			.value_(remoteCount.value);
			
		controller = Updater(remoteCount, { |count|
			{
				view.value_(count.value);
				view.background_( if( count.value != -1 ) { green } { red} );
			}.defer
		})
	}
	
	remove{
		controller.remove
	}
}

	
		