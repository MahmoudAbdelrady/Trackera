import { Form, Input } from "antd";
import classes from "./scss/input-field.module.css";
import type { InputFieldProps } from "../../shared/types";

const InputField = (props: InputFieldProps) => {
  const fieldProps = {
    prefix: props.icon,
    placeholder: props.placeholder,
    className: classes.input_field,
    name: props.name,
    value: props.value,
    onChange: props.onChange,
    onBlur: props.onBlur,
    disabled: props.disabled,
    type: props.type,
  };
  return (
    <div className={classes.input_group}>
      <div className={classes.input_label}>{props.label}</div>
      <Form.Item
        style={{ marginBottom: 0, width: "100%" }}
        validateStatus={props.error ? "error" : ""}
        help={props.error ? props.error : ""}
      >
        {props.type === "password" ? (
          <Input.Password {...fieldProps} />
        ) : (
          <Input {...fieldProps} />
        )}
      </Form.Item>
    </div>
  );
};

export default InputField;
