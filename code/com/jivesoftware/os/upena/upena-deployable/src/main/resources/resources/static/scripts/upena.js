window.$ = window.jQuery;

window.upena = {};

upena.hs = {

    installed: null,
    queuedUninstall: null,

    init: function() {
        $('.upena-hover-selector').each(function(i, selector) {
            var $selector = $(selector);
            $selector.focus(function() {
                if (selector == upena.hs.installed.selector) {
                    upena.hs.cancelUninstall();
                }
            });
            $selector.blur(function() {
                upena.hs.uninstall();
            })
        });

        $('input[type=text].upena-hs-field').each(function(i, input) {
            var $input = $(input);
            var selector = $input.data('upena-selector');
            $input.focus(function() {
                upena.hs.install(selector, $input, function(which) {
                    $input.val(which);
                });
            });
            $input.blur(function() {
                upena.hs.queuedUninstall = setTimeout(function() {
                    upena.hs.uninstall($input);
                }, 200);
            });
        });

        $('.upena-hover-selector a').each(function(i, a) {
            var $a = $(a);
            var selector = $a.data('upena-selector');
            var value = $a.data('upena-value');
            $a.click(function() {
                upena.hs.picked(value);
                return false;
            });
        });
    },

    install: function(selector, $focuser, callback) {
        upena.hs.cancelUninstall();
        upena.hs.uninstall();
        upena.hs.installed = {
            selector: selector,
            focuser: $focuser,
            callback: callback
        };

        var $selector = $(selector);
        var offset = $focuser.offset();
        var width = $focuser.width();
        $selector.show();
        $selector.offset({
            left: offset.left + width + 20,
            top: offset.top
        });
    },

    uninstall: function($focuser) {
        if (!upena.hs.installed || $focuser && upena.hs.installed.focuser != $focuser) {
            return;
        }
        $(upena.hs.installed.selector).hide();
        upena.hs.installed = null;
    },

    cancelUninstall: function() {
        if (upena.hs.queuedUninstall) {
            clearTimeout(upena.hs.queuedUninstall);
        }
    },

    picked: function(which) {
        if (upena.hs.installed) {
            upena.hs.installed.callback(which);
            upena.hs.uninstall();
        }
    }
};

upena.instances = {

    init: function() {
    }
};

$(document).ready(function() {
    if ($('.upena-hover-selector').length) {
        upena.hs.init();
    }
    if ($('#upena-instances').length) {
        upena.instances.init();
    }
});