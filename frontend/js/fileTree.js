(function ( $ ) {
    /**
     * Default configuration of the plugin
     */
    var defaultConfig = {
        /**
         * Object which handles changes on file tree state.
         *
         * Must provide the following methods:
         * - `addNodes(paths)` which is called when expandable
         *   file is opened (expanded)
         * - `removeNode(path)` which is called when expandable
         *   file is closed (collapsed)
         * - `getCurrentState()` which returns tree representing
         *   current state
         *
         * State tree must be built from nodes represented by
         * the following structure:
         *
         * ```
         * {
         *     fileData: {path: "<file_path>", type: "<file_type>", isExpandable: <boolean>},
         *     children: <array_of_subnodes>
         * }
         * ```
         */
        stateHolder: {},

        /**
         * Object which provides loadable data, must provide
         * only one method: `list(path)` which returns array
         * of `FileData` objects:
         *
         * ```
         * {
         *     path: "<file_path>",
         *     type: "<file_type>",
         *     isExpandable: <boolean>
         * }
         * ```
         */
        dataProvider: {},

        /**
         * Map of file MIME type to appropriate icons classes
         */
        typeToIconClassMap: {
            "": ""
        }
    }

    /**
     * Plugin entry point. Defines JQuery function `fileTree`
     * which can be used to initialize tree view on specific
     * DOM element. Optionally, gets configuration which can
     * override default configuration (see `defaultConfig` for
     * details).
     */
    $.fn.fileTree = function(config) {
        return this;
    }

}( jQuery ));