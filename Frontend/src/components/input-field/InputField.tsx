import { Form, Input } from "antd";
import classes from "./scss/input-field.module.css";

interface InputFieldProps {
  label?: string;
  icon?: React.ReactNode;
  type: string;
  name: string;
  placeholder?: string;
  value?: string;
  onChange?: (event: React.ChangeEvent<HTMLInputElement>) => void;
  onBlur?: (event: React.FocusEvent<HTMLInputElement>) => void;
  error?: string;
  disabled?: boolean;
}

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

  const handlePreventPasswordCopyPaste = (event: React.ClipboardEvent<HTMLInputElement>) => {
    event.preventDefault();
  };

  return (
    <div className={classes.input_group}>
      {props.label && <div className={classes.input_label}>{props.label}</div>}

      <Form.Item
        style={{ marginBottom: 0, width: "100%" }}
        validateStatus={props.error ? "error" : ""}
        help={props.error ? props.error : ""}
      >
        {props.type === "password" ? (
          <Input.Password
            {...fieldProps}
            onCut={handlePreventPasswordCopyPaste}
            onCopy={handlePreventPasswordCopyPaste}
            onPaste={handlePreventPasswordCopyPaste}
          />
        ) : (
          <Input {...fieldProps} />
        )}
      </Form.Item>
    </div>
  );
};

export default InputField;
