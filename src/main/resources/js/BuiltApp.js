define("Group", [ "backbone" ], function(Backbone) {
	return Backbone.Model.extend({
		urlRoot : "/stash/rest/privaterepos/1.0/group",
		idAttribute : "name"
	})
});
define("Groups", [ "backbone", "Group" ], function(Backbone, Group) {
	return Backbone.Collection.extend({
		url : "/stash/rest/privaterepos/1.0/groups",
		model : Group
	})
});
define("User", [ "backbone" ], function(Backbone) {
	return Backbone.Model.extend({
		urlRoot : "/stash/rest/privaterepos/1.0/user",
		idAttribute : "name"
	})
});
define("Users", [ "backbone", "User" ], function(Backbone, User) {
	return Backbone.Collection.extend({
		url : "/stash/rest/privaterepos/1.0/users",
		model : User
	})
});
define("GroupRow", [ "backbone" ], function(Backbone) {
	return Backbone.View.extend({
		events : {
			"click .delete" : "onDelete"
		},
		onDelete : function(e) {
		},
		render : function() {
			return this
		}
	})
});
define("GroupsTable", [ "Table", "GroupRow" ], function(Table, GroupRow) {
	return Table.extend({
		itemView : GroupRow
	})
});
define("Table", [ "backbone", "underscore" ], function(Backbone, _) {
	return Backbone.View.extend({
		tagName : "table",
		initialize : function(options) {
			_.bindAll(this, "addChildView");
			this.collection = options.collection;
			this.collection.on("add", this.addChildView)
		},
		addChildView : function(model) {
			var itemViewClass = this.itemView;
			this.$el.append(new itemViewClass({
				model : model
			}).render().el)
		},
		render : function() {
			return this
		}
	})
});
define("UserRow", [ "backbone" ], function(Backbone) {
	return Backbone.View.extend({
		tagName : "tr",
		template : PrivateRepos.userRow,
		events : {
			"click .delete" : "onDelete"
		},
		onDelete : function(e) {
		},
		render : function() {
			this.el.innerHTML = this.template({
				user : this.model.toJSON()
			});
			return this
		}
	})
});
define("UsersTable", [ "Table", "UserRow" ], function(Table, UserRow) {
	return Table.extend({
		itemView : UserRow
	})
});
define("PrivateRepos", [ "jquery", "UsersTable", "Users", "GroupsTable",
		"Groups" ], function($, UsersTable, Users, GroupsTable, Groups) {
	var constr = function(opts) {
		this.initialize(opts)
	};
	_.extend(constr.prototype, {
		initialize : function(opts) {
		},
		start : function() {
			this.startUsersTable();
			this.startGroupsTable()
		},
		startUsersTable : function() {
			var users = new Users;
			var usersTable = new UsersTable({
				collection : users
			});
			$("#users-table").html(usersTable.render().el);
			users.fetch()
		},
		startGroupsTable : function() {
			var groups = new Users;
			var groupsTable = new GroupsTable({
				collection : groups
			});
			$("#groups-table").html(groupsTable.render().el);
			groups.fetch()
		}
	});
	return constr
});
AJS.$(document).ready(function($) {
	require([ "PrivateRepos" ], function(PrivateRepos) {
		console.log("Starting Private Repos");
		var privateRepos = new PrivateRepos;
		privateRepos.start()
	})
}(AJS.$));