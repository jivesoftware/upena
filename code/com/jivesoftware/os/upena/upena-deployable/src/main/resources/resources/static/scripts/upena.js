window.$ = window.jQuery;

window.upena = {};

upena.hs = {

    installed: null,
    queuedUninstall: null,

    init: function () {
        $('input[type=text].upena-hs-field').each(function (i, input) {
            var $inputName = $(input);
            var $inputKey = $(input).next('input[type=hidden]');
            var endpoint = $inputName.data('upenaLookup');
            $inputName.focus(function () {
                upena.hs.install($inputKey, $inputName, function (key, name) {
                    $inputKey.val(key);
                    $inputName.val(name);
                });
                upena.hs.lookup(endpoint, $inputName.val());
            });
            $inputName.on('input', function () {
                $inputKey.val('');
                upena.hs.lookup(endpoint, $inputName.val());
            });
            $inputName.blur(function () {
                upena.hs.queuedUninstall = setTimeout(function () {
                    upena.hs.uninstall($inputName);
                }, 200);
            });
        });
    },

    install: function ($inputKey, $inputName, callback) {
        upena.hs.cancelUninstall();
        upena.hs.uninstall();

        var $selector = upena.hs.makeSelector();
        $('body').append($selector);

        upena.hs.installed = {
            selector: $selector,
            inputKey: $inputKey,
            inputName: $inputName,
            callback: callback,
            ready: false
        };

        $inputName.removeClass('upena-hs-field-broken');

        var offset = $inputName.offset();
        var height = $inputName.height();
        $selector.show();
        $selector.offset({
            left: offset.left,
            top: offset.top + height + 10
        });
    },

    uninstall: function ($inputName) {
        if (!upena.hs.installed || $inputName && upena.hs.installed.inputName != $inputName) {
            return;
        }

        $inputName = $inputName || upena.hs.installed.inputName;
        var $inputKey = upena.hs.installed.inputKey;
        var name = $inputName.val();
        var found = false;

        var $selector = upena.hs.installed.selector;
        $selector.find('a').each(function (i) {
            var $a = $(this);
            if ($a.data('upenaName') == name) {
                var key = $a.data('upenaKey');
                $inputKey.val(key);
                found = true;
                return false;
            }
        });

        if (!found) {
            $inputName.addClass('upena-hs-field-broken');
            upena.hs.installed.inputKey.val('');
        }

        $selector.remove();
        upena.hs.installed = null;
    },

    makeSelector: function () {
        var $selector = $('<div>').addClass("upena-hs-selector");
        $selector.focus(function () {
            if (selector == upena.hs.installed.selector) {
                upena.hs.cancelUninstall();
            }
        });
        $selector.blur(function () {
            upena.hs.uninstall();
        });
        return $selector;
    },

    cancelUninstall: function () {
        if (upena.hs.queuedUninstall) {
            clearTimeout(upena.hs.queuedUninstall);
        }
    },

    picked: function (key, name) {
        if (upena.hs.installed) {
            upena.hs.installed.callback(key, name);
            upena.hs.uninstall();
        }
    },

    lookup: function (endpoint, contains) {
        var $selector = upena.hs.installed.selector;
        $.ajax(endpoint, {data: {'contains': contains}})
            .done(function (data) {
                if (!upena.hs.installed || upena.hs.installed.selector != $selector) {
                    // selector changed during the query
                    return;
                }
                if (data.length) {
                    $selector.empty();
                    for (var i = 0; i < data.length; i++) {
                        $selector.append(
                            "<a href='#'" +
                            " class='upena-hs-choice'" +
                            " data-upena-key='" + data[i].key + "'" +
                            " data-upena-name='" + data[i].name + "'>" + data[i].name + "</a><br/>");
                    }
                    upena.hs.link($selector);
                    upena.hs.installed.ready = true;
                } else {
                    $selector.html("<em>No matches</em>");
                }
            });
    },

    link: function ($selector) {
        $selector.find('a').each(function (i, a) {
            var $a = $(a);
            var key = $a.data('upenaKey');
            var name = $a.data('upenaName');
            $a.click(function () {
                upena.hs.picked(key, name);
                return false;
            });
        });
    }
};

upena.instances = {

    init: function () {
    }
};

$(document).ready(function () {
    if ($('.upena-hs-field').length) {
        upena.hs.init();
    }
    if ($('#upena-instances').length) {
        upena.instances.init();
    }
});