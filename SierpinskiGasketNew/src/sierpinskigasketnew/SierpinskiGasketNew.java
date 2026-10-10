package sierpinskigasketnew;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import java.io.InputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class SierpinskiGasketNew implements GLEventListener {

    // Increase this value to add more fractal detail.
    private static final int DEPTH = 6;

    private final List<Float> vertices = new ArrayList<>();

    private int vao;
    private int vbo;
    private int shaderProgram;
    private int vertexCount;

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            GLProfile profile = GLProfile.get(GLProfile.GL3);

            GLCapabilities capabilities =
                    new GLCapabilities(profile);

            GLCanvas canvas = new GLCanvas(capabilities);

            canvas.addGLEventListener(
                    new SierpinskiGasketNew());

            JFrame frame = new JFrame(
                    "Sierpinski Gasket – JOGL");

            frame.setDefaultCloseOperation(
                    JFrame.EXIT_ON_CLOSE);

            frame.add(canvas);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    @Override
    public void init(GLAutoDrawable drawable) {

        GL3 gl = drawable.getGL().getGL3();

        gl.glClearColor(0.05f, 0.05f, 0.12f, 1.0f);

        // Main triangle coordinates.
        float[] top = {0.0f, 0.85f};
        float[] left = {-0.85f, -0.75f};
        float[] right = {0.85f, -0.75f};

        divideTriangle(top, left, right, DEPTH);

        vertexCount = vertices.size() / 3;

        float[] data = new float[vertices.size()];

        for (int i = 0; i < vertices.size(); i++) {
            data[i] = vertices.get(i);
        }

        // Create vertex array and vertex buffer.
        IntBuffer vaoBuffer = IntBuffer.allocate(1);
        IntBuffer vboBuffer = IntBuffer.allocate(1);

        gl.glGenVertexArrays(1, vaoBuffer);
        gl.glGenBuffers(1, vboBuffer);

        vao = vaoBuffer.get(0);
        vbo = vboBuffer.get(0);

        gl.glBindVertexArray(vao);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);

        FloatBuffer vertexBuffer = FloatBuffer.wrap(data);

        gl.glBufferData(
                GL.GL_ARRAY_BUFFER,
                (long) data.length * Float.BYTES,
                vertexBuffer,
                GL.GL_STATIC_DRAW
        );

        gl.glVertexAttribPointer(
                0, 3, GL.GL_FLOAT,
                false, 0, 0L
        );

        gl.glEnableVertexAttribArray(0);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        gl.glBindVertexArray(0);

        // Compile and link the GLSL shaders.
        String vertexSource =
                loadShader("vertexShader.glsl");

        String fragmentSource =
                loadShader("fragmentShader.glsl");

        int vertexShader = compileShader(
                gl, GL3.GL_VERTEX_SHADER, vertexSource);

        int fragmentShader = compileShader(
                gl, GL3.GL_FRAGMENT_SHADER, fragmentSource);

        shaderProgram = gl.glCreateProgram();

        gl.glAttachShader(shaderProgram, vertexShader);
        gl.glAttachShader(shaderProgram, fragmentShader);

        gl.glLinkProgram(shaderProgram);

        checkProgram(gl, shaderProgram);

        gl.glDeleteShader(vertexShader);
        gl.glDeleteShader(fragmentShader);

        System.out.println("Sierpinski Gasket initialized.");
        System.out.println("Recursion depth: " + DEPTH);
        System.out.println("Vertices: " + vertexCount);
    }

    // Recursively subdivide the triangle.
    private void divideTriangle(
            float[] a, float[] b, float[] c, int depth) {

        if (depth == 0) {
            addTriangle(a, b, c);
            return;
        }

        float[] ab = midpoint(a, b);
        float[] ac = midpoint(a, c);
        float[] bc = midpoint(b, c);

        divideTriangle(a, ab, ac, depth - 1);
        divideTriangle(ab, b, bc, depth - 1);
        divideTriangle(ac, bc, c, depth - 1);
    }

    private float[] midpoint(float[] a, float[] b) {
        return new float[]{
            (a[0] + b[0]) / 2.0f,
            (a[1] + b[1]) / 2.0f
        };
    }

    private void addTriangle(
            float[] a, float[] b, float[] c) {

        addVertex(a);
        addVertex(b);
        addVertex(c);
    }

    private void addVertex(float[] point) {
        vertices.add(point[0]);
        vertices.add(point[1]);
        vertices.add(0.0f);
    }

    // Read GLSL files from the package resources.
    private String loadShader(String filename) {

        try (InputStream stream =
                getClass().getResourceAsStream(filename)) {

            if (stream == null) {
                throw new RuntimeException(
                        "Shader file not found: " + filename);
            }

            byte[] bytes = stream.readAllBytes();

            return new String(
                    bytes, StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to read shader: " + filename, e);
        }
    }

    private int compileShader(
            GL3 gl, int type, String source) {

        int shader = gl.glCreateShader(type);

        String[] sources = {source};
        int[] lengths = {source.length()};

        gl.glShaderSource(
                shader, 1, sources, lengths, 0);

        gl.glCompileShader(shader);

        int[] status = new int[1];

        gl.glGetShaderiv(
                shader, GL3.GL_COMPILE_STATUS,
                status, 0);

        if (status[0] == GL.GL_FALSE) {

            int[] logLength = new int[1];

            gl.glGetShaderiv(
                    shader, GL3.GL_INFO_LOG_LENGTH,
                    logLength, 0);

            byte[] log = new byte[Math.max(logLength[0], 1)];

            gl.glGetShaderInfoLog(
                    shader, log.length,
                    null, 0, log, 0);

            throw new RuntimeException(
                    "Shader compilation failed: "
                    + new String(log, StandardCharsets.UTF_8));
        }

        return shader;
    }

    private void checkProgram(GL3 gl, int program) {

        int[] status = new int[1];

        gl.glGetProgramiv(
                program, GL3.GL_LINK_STATUS,
                status, 0);

        if (status[0] == GL.GL_FALSE) {

            int[] logLength = new int[1];

            gl.glGetProgramiv(
                    program, GL3.GL_INFO_LOG_LENGTH,
                    logLength, 0);

            byte[] log = new byte[Math.max(logLength[0], 1)];

            gl.glGetProgramInfoLog(
                    program, log.length,
                    null, 0, log, 0);

            throw new RuntimeException(
                    "Shader linking failed: "
                    + new String(log, StandardCharsets.UTF_8));
        }
    }

    @Override
    public void display(GLAutoDrawable drawable) {

        GL3 gl = drawable.getGL().getGL3();

        gl.glClear(GL.GL_COLOR_BUFFER_BIT);

        gl.glUseProgram(shaderProgram);
        gl.glBindVertexArray(vao);

        gl.glDrawArrays(
                GL.GL_TRIANGLES, 0, vertexCount);

        gl.glBindVertexArray(0);
        gl.glUseProgram(0);
    }

    @Override
    public void reshape(
            GLAutoDrawable drawable,
            int x, int y, int width, int height) {

        GL3 gl = drawable.getGL().getGL3();

        gl.glViewport(0, 0, width, height);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {

        GL3 gl = drawable.getGL().getGL3();

        if (shaderProgram != 0) {
            gl.glDeleteProgram(shaderProgram);
        }

        if (vbo != 0) {
            int[] buffers = {vbo};
            gl.glDeleteBuffers(1, buffers, 0);
        }

        if (vao != 0) {
            int[] arrays = {vao};
            gl.glDeleteVertexArrays(1, arrays, 0);
        }
    }
}
