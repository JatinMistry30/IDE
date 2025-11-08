# Java-Based IDE Project

## What You're Building
A Java-based IDE that can write and run Python, Java, and JavaScript code with syntax highlighting, code completion, and error detection.

---

## Development Checklist (Step by Step)

### STEP 1: Create Basic GUI Application with JavaFX
- [ ] Create main JavaFX Stage with window title "My IDE"
- [ ] Create BorderPane as root layout
- [ ] Add menu bar at top (File, Edit, Run, Help menus)
- [ ] Add toolbar with buttons (New, Open, Save, Run, Stop)
- [ ] Add status bar at bottom (showing file info, cursor position)
- [ ] Create split pane layout: left side for file explorer, right side for editor
- [ ] Run the application - should display empty window with menu

### STEP 2: Build File Explorer Panel (Left Side)
- [ ] Create TreeView component for file structure
- [ ] Display folder and file hierarchy
- [ ] Add ability to expand/collapse folders
- [ ] Show different icons for files vs folders
- [ ] Click on file to select it
- [ ] Display current project path
- [ ] Make it functional (not just visual)

### STEP 3: Build Code Editor Panel (Right Side)
- [ ] Create TextArea for writing code
- [ ] Set monospace font (Courier New, 12pt)
- [ ] Add line numbers on left side
- [ ] Support multi-tab interface (switch between open files)
- [ ] Make text editable
- [ ] Show cursor position in status bar
- [ ] Add keyboard shortcuts (Ctrl+S for save, Ctrl+A for select all)

### STEP 4: Build Console Output Panel (Below Editor)
- [ ] Create TextArea for displaying output
- [ ] Make console read-only (users can't type in it)
- [ ] Show program output here
- [ ] Show error messages in red
- [ ] Add clear console button
- [ ] Separate console from editor with divider

### STEP 5: Implement File Operations
- [ ] Create "New File" button - creates blank file
- [ ] Create "Open File" button - opens file chooser dialog
- [ ] Create "Save File" button - saves current file to disk
- [ ] Create "Save As" button - save with different name
- [ ] Create "Delete File" button - delete selected file
- [ ] Update file explorer after each operation
- [ ] Show unsaved changes indicator (*)

### STEP 6: Implement Code Execution for Python
- [ ] Create button "Run" in toolbar
- [ ] Detect if current file is .py file
- [ ] Execute Python file using ProcessBuilder("python3", filename)
- [ ] Capture output from program
- [ ] Display output in console
- [ ] Capture error messages
- [ ] Display execution time
- [ ] Add "Stop" button to kill running process

### STEP 7: Implement Code Execution for JavaScript (Node.js)
- [ ] Detect if current file is .js file
- [ ] Execute JavaScript using ProcessBuilder("node", filename)
- [ ] Capture console.log() output
- [ ] Display in console panel
- [ ] Handle Node.js errors
- [ ] Allow user input/output testing

### STEP 8: Implement Code Execution for Java
- [ ] Detect if current file is .java file
- [ ] Compile using ProcessBuilder("javac", filename)
- [ ] Extract class name from file
- [ ] Run compiled class using ProcessBuilder("java", classname)
- [ ] Show compilation errors if any
- [ ] Display program output
- [ ] Handle Java-specific errors

### STEP 9: Add Syntax Highlighting for Python
- [ ] Identify Python keywords (def, class, if, for, while, import, etc.)
- [ ] Color keywords blue
- [ ] Identify strings (single and double quotes) - color orange
- [ ] Identify comments (lines starting with #) - color green
- [ ] Identify numbers - color light green
- [ ] Update highlighting in real-time as user types
- [ ] Apply to all Python files

### STEP 10: Add Syntax Highlighting for Java
- [ ] Identify Java keywords (public, private, class, void, int, String, etc.)
- [ ] Color keywords blue
- [ ] Identify strings - color orange
- [ ] Identify comments (// and /* */) - color green
- [ ] Identify numbers - color light green
- [ ] Identify class names and method names
- [ ] Update in real-time while typing
- [ ] Apply to all Java files

### STEP 11: Add Syntax Highlighting for JavaScript
- [ ] Identify JS keywords (const, let, function, async, await, etc.)
- [ ] Color keywords blue
- [ ] Identify strings (single, double, backticks) - color orange
- [ ] Identify comments (// and /* */) - color green
- [ ] Identify template literals
- [ ] Identify numbers - color light green
- [ ] Update in real-time while typing
- [ ] Apply to all JavaScript files

### STEP 12: Add Code Completion (Auto-Suggestions)
- [ ] When user types partial keyword and presses Ctrl+Space
- [ ] Show list of matching suggestions
- [ ] For Python: show print(), len(), range(), for, if, while, def, class, etc.
- [ ] For Java: show public, private, class, void, int, String, for, while, etc.
- [ ] For JavaScript: show function, const, let, console.log(), etc.
- [ ] Display suggestions in dropdown
- [ ] Allow user to select and insert suggestion
- [ ] Dismiss on Escape key

### STEP 13: Add Error Detection/Highlighting
- [ ] Scan Python code for common errors (missing colons after if/for/def)
- [ ] Scan Java code for missing semicolons
- [ ] Scan JavaScript code for common mistakes
- [ ] Show errors with red underline
- [ ] Display error message on hover
- [ ] Show all errors in "Problems" panel
- [ ] Update errors in real-time as user types
- [ ] Make errors clickable to jump to line

### STEP 14: Add Find & Replace Functionality
- [ ] Add Find toolbar (Ctrl+F)
- [ ] Show find dialog with text field
- [ ] Highlight all matches in editor
- [ ] Show match count (e.g., "3 of 5 matches")
- [ ] Navigate through matches with arrow buttons
- [ ] Add Replace dialog (Ctrl+H)
- [ ] Replace single or replace all
- [ ] Close find toolbar with Escape key

### STEP 15: Add Settings/Preferences
- [ ] Create Settings menu
- [ ] Add font selection dropdown
- [ ] Add font size slider
- [ ] Add theme selector (Dark/Light)
- [ ] Add tab size selector (2, 4, 8 spaces)
- [ ] Add line wrapping toggle
- [ ] Add auto-save toggle
- [ ] Save preferences to config file
- [ ] Load preferences on startup

### STEP 16: Implement Dark Mode & Light Mode
- [ ] Create dark theme CSS
- [ ] Create light theme CSS
- [ ] Apply theme to entire application
- [ ] Change editor background (dark: dark gray, light: white)
- [ ] Change text color (dark: light gray, light: black)
- [ ] Change syntax highlighting colors for both themes
- [ ] Save theme preference
- [ ] Switch theme without restarting

### STEP 17: Add Auto-Save Feature
- [ ] Auto-save file every N seconds (configurable)
- [ ] Only save if file has unsaved changes
- [ ] Show "Saving..." indicator briefly
- [ ] Update timestamp in status bar
- [ ] Allow user to disable auto-save

### STEP 18: Add Line Wrapping Toggle
- [ ] Add "Word Wrap" checkbox in View menu
- [ ] When enabled, long lines wrap to next visual line
- [ ] When disabled, horizontal scrollbar appears
- [ ] Remember preference in settings
- [ ] Apply to all open files

### STEP 19: Add Keyboard Shortcuts
- [ ] Ctrl+N = New File
- [ ] Ctrl+O = Open File
- [ ] Ctrl+S = Save File
- [ ] Ctrl+H = Find & Replace
- [ ] Ctrl+/ = Toggle comment (optional)
- [ ] Ctrl++ = Increase font size
- [ ] Ctrl+- = Decrease font size
- [ ] F5 or Ctrl+R = Run code
- [ ] F6 = Stop execution
- [ ] Display shortcuts in menu items

### STEP 20: Add Recent Files List
- [ ] Track last 10 files opened
- [ ] Display in File menu under "Recent Files"
- [ ] Click to reopen quickly
- [ ] Show file path in tooltip
- [ ] Save recent files to config file
- [ ] Load on startup

### STEP 21: Add File Type Icons
- [ ] Show different icon for .py files (Python icon)
- [ ] Show different icon for .java files (Java icon)
- [ ] Show different icon for .js files (JavaScript icon)
- [ ] Show different icon for folders
- [ ] Use simple colored squares or download icon pack

### STEP 22: Add Project Management
- [ ] Create "New Project" dialog
- [ ] Allow user to select project location
- [ ] Create folder structure
- [ ] Load project on startup
- [ ] Show project name in window title
- [ ] Save project config file

### STEP 23: Add Input/Output Handling
- [ ] Create input panel for user to provide input to programs
- [ ] Allow typing input before running program
- [ ] Pass input to running process
- [ ] Display combined output and input in console
- [ ] Show input in different color than output

### STEP 24: Add Program Execution Time Display
- [ ] Measure time before and after execution
- [ ] Calculate execution time in milliseconds
- [ ] Display "Execution Time: 125ms" in console
- [ ] Show even if program has no output
- [ ] Handle very fast programs (0ms)

### STEP 25: Add Language Auto-Detection
- [ ] Detect language by file extension (.py, .java, .js)
- [ ] Set syntax highlighting automatically
- [ ] Set correct executor automatically
- [ ] Show detected language in status bar
- [ ] Allow manual language override

### STEP 26: Polish and Testing
- [ ] Test opening and running Python files
- [ ] Test opening and running JavaScript files
- [ ] Test opening and running Java files
- [ ] Test file operations (new, open, save, delete)
- [ ] Test syntax highlighting for all languages
- [ ] Test error detection
- [ ] Test find & replace
- [ ] Test theme switching
- [ ] Test all keyboard shortcuts
- [ ] Test console output
- [ ] Handle edge cases and errors

### STEP 27: Create GitHub Repository
- [ ] Initialize git in project
- [ ] Create .gitignore file
- [ ] Write README.md explaining features
- [ ] Add screenshots
- [ ] Create first commit
- [ ] Push to GitHub
- [ ] Make repository public
- [ ] Add project description

### STEP 28: Document Code
- [ ] Add JavaDoc comments to all classes
- [ ] Add inline comments explaining complex logic
- [ ] Create architecture documentation
- [ ] Explain design decisions
- [ ] Create user guide
- [ ] Document all classes and methods

---

## Implementation Order Summary

1. Basic GUI with JavaFX
2. File explorer panel
3. Code editor panel
4. Console output panel
5. File operations
6. Python execution
7. JavaScript execution
8. Java execution
9. Python syntax highlighting
10. Java syntax highlighting
11. JavaScript syntax highlighting
12. Code completion
13. Error detection
14. Find & Replace
15. Settings/Preferences
16. Dark/Light themes
17. Auto-save
18. Line wrapping
19. Keyboard shortcuts
20. Recent files
21. File icons
22. Project management
23. Input/Output handling
24. Execution time display
25. Language auto-detection
26. Polish and testing
27. GitHub repository
28. Documentation

---

## What to Do Right Now

**Start with STEP 1:** Create a basic JavaFX window with menu bar and button. Don't worry about functionality yet—just get the UI showing.

Once that works, move to STEP 2 and add the file explorer panel.

Take it one step at a time. Each step builds on the previous one.

Good luck! 🚀