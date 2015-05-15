//model
Template = can.Model({
	findAll : 'GET admin/templates',
	findOne : 'POST admin/templates',
	destroy : 'POST admin/templates/delete',
	create : 'POST admin/templates/update',
	update : 'POST admin/templates/update'
}, {});

// model
Settings = can.Model({
	findOne : 'GET admin/settings',
	create : 'POST admin/settings/update',
	update : 'POST admin/settings/update'
}, {});

// controller
AdminSettingsPage = can
		.Control({

			"init" : function(element, options) {
				var that = this;
				Template.findAll({}, function(snippets) {

					Settings.findOne({}, function(settings) {

						that.element.html(can.view('views/admin/settings.ejs',
								{
									snippets : snippets,
									settings : settings
								}));
						that.settings = settings;
						$("#content").fadeIn();

					});
				}, function(message) {
					new ErrorPage(that.element, {
						status : message.statusText,
						message : message.responseText
					});
				});
			},

			'submit' : function() {

				this.settings.attr('name',this.element.find("[name='name']").val());
				
				this.settings.attr('hadoopPath',this.element.find("[name='hadoopPath']").val());
				this.settings.attr('userApp',this.element.find("[name='userApp']").val());
				this.settings.attr('adminApp',this.element.find("[name='adminApp']").val());

				this.settings.attr('mail-smtp',this.element.find("[name='mail-smtp']").val());
				this.settings.attr('mail-port',this.element.find("[name='mail-port']").val());
				this.settings.attr('mail-user',this.element.find("[name='mail-user']").val());
				this.settings.attr('mail-password',this.element.find("[name='mail-password']").val());
				this.settings.attr('mail-name',this.element.find("[name='mail-name']").val());
				this.settings.attr('piggene',this.element.find("[name='piggene']").val());

				
				this.settings.save();

				bootbox.animate(false);
				bootbox.alert("Settings updated.");

				return false;
			},

			'.icon-pencil click' : function(el, ev) {

				template = el.parent().parent().data('template');
				bootbox.animate(false);
				var oldText = template.attr('text');
				bootbox
						.confirm(
								'<h4>'
										+ template.attr('key')
										+ '</h4><form><textarea class="field span5" id="message" rows="10" name="message" width="30" height="20">'
										+ oldText + '</textarea></form>',
								function(result) {
									if (result) {
										var text = $('#message').val();
										template.attr('text', text);
										template.save();
									}
								});

			}

		});