# KNIME Deep Learning - TensorFlow Integration

## Python bindings

The TensorFlow integration can be used with the _DL Python Network_ Nodes which allow you to create, train and modify a TensorFlow model using the powerful TensorFlow Python API.

The KNIME TensorFlow Integration provides a `TFModel` object to Python whenever a model is loaded into Python and requires you to set a `TFModel` object whenever a model should be returned to KNIME for further usage.

### Examples

__Create a TensorFlow model:__

```
import tensorflow as tf
from TFModel import TFModel

# Create a graph
graph = tf.Graph()

# Set the graph as default -> Create every tensor in this graph
with graph.as_default():

    # Create an input tensor
    x = tf.placeholder(tf.float32, shape=(None,4))

    # define the graph...

    # Create an output tensor
    y = tf.nn.softmax(last_layer)

# Create the output network
output_network = TFModel(inputs={ 'input': x }, outputs={ 'output': y }, graph=graph)
```

__Use/Train/Edit a TensorFlow model:__

```
import tensorflow as tf
from TFModel import TFModel

# Use the session from the TFModel
with input_network.session as sess:

    # Get the input tensor
    x = input_network.inputs['input']

    # Get the output tensor
    y = input_network.outputs['output']

    # Use/Train/Edit the model...

    # Create the output network
    # NOTE: The whole session is passed to the model (to save the variables)
    #       This needs to be called before the session is closed
    output_network = TFModel(inputs={ 'input': x }, outputs={ 'output': y }, session=sess)
```
